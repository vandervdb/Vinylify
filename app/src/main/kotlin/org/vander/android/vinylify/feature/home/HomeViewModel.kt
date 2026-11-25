package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.feature.ContentKind
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.Album
import org.vander.spotify.model.AlbumCollection
import org.vander.spotify.model.Artist
import org.vander.spotify.model.ArtistCollection
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistCollection
import javax.inject.Inject

/**
 * Accueil : albums enregistrés + playlists, filtrables, avec un héros « Reprendre l'écoute ».
 *
 * Le ViewModel ne tient aucune copie des données : [SpotifyClient.savedAlbums] et
 * [SpotifyClient.playlists] sont déjà des `StateFlow` de la lib, il les combine. Seul le
 * filtre est un état propre à l'écran.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val filter = MutableStateFlow(HomeFilter.All)

        private val loadFailed = MutableStateFlow(false)
        private var loadJob: Job? = null

        val state: StateFlow<HomeUiState> =
            combine(spotify.savedAlbums, spotify.playlists, spotify.followedArtists, filter, player.nowPlaying, ::buildState)
                .combine(loadFailed) { ui, failed -> ui.copy(loadFailed = failed) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

        init {
            refreshWhenSignedIn()
        }

        /** Relance le chargement, à la demande de l'utilisateur après un échec. */
        fun onRetry() = refreshWhenSignedIn()

        fun onFilterSelect(value: HomeFilter) = filter.update { value }

        fun onResumeListen() {
            val first = spotify.savedAlbums.value.items.firstOrNull() ?: return
            viewModelScope.launch { player.playAlbum(first) }
        }

        /**
         * Attend l'autorisation avant de charger : la session peut être restaurée depuis le
         * DataStore après la création du ViewModel, et un appel sans jeton échouerait en
         * `NotSignedIn`.
         *
         * Un chargement qui échoue est retenté : juste après la connexion, le premier appel peut partir
         * avant que le jeton soit prêt et laisser une bibliothèque vide sans erreur visible.
         */
        private fun refreshWhenSignedIn() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    loadFailed.value = false
                    spotify.session.first { it is SessionState.Authorized }
                    repeat(MAX_LOAD_ATTEMPTS) { attempt ->
                        if (loadLibrary()) return@launch
                        if (attempt < MAX_LOAD_ATTEMPTS - 1) delay(RETRY_DELAY_MS * (attempt + 1))
                    }
                    loadFailed.value = true
                }
        }

        /** Vrai si les albums et les playlists sont chargés. */
        private suspend fun loadLibrary(): Boolean {
            val albums = spotify.refreshSavedAlbums()
            val playlists = spotify.refreshPlaylists()
            // Peut échouer en 403 avec un jeton émis avant l'ajout du scope `user-follow-read` :
            // l'onglet Artistes reste alors vide jusqu'à la prochaine connexion. Ce n'est pas retenté.
            spotify.refreshFollowedArtists()
            return albums.isSuccess && playlists.isSuccess
        }

        private fun buildState(
            albums: AlbumCollection,
            playlists: PlaylistCollection,
            artists: ArtistCollection,
            selected: HomeFilter,
            nowPlaying: NowPlaying?,
        ): HomeUiState {
            val albumTiles = albums.items.map { it.toTile() }
            val playlistTiles = playlists.items.map { it.toTile() }
            val artistTiles = artists.items.map { it.toTile() }
            val (tiles, count) =
                when (selected) {
                    HomeFilter.All ->
                        (albumTiles + playlistTiles + artistTiles) to (albums.total + playlists.total + artists.total)
                    HomeFilter.Albums -> albumTiles to albums.total
                    HomeFilter.Playlists -> playlistTiles to playlists.total
                    HomeFilter.Artists -> artistTiles to artists.total
                }
            return HomeUiState(
                tiles = tiles,
                libraryCount = count,
                playingTileId = playingAlbumId(albums, nowPlaying),
                // « Reprendre l'écoute » propose un album : hors sujet sur Playlists et Artistes.
                resume =
                    albums.items
                        .firstOrNull()
                        ?.toResume()
                        ?.takeIf { selected == HomeFilter.All || selected == HomeFilter.Albums },
                selectedFilter = selected,
            )
        }

        /**
         * `NowPlaying` ne porte que le nom de l'album, pas son identifiant : on rapproche par
         * nom. Approximation assumée tant que le SDK n'expose pas l'URI de contexte courante.
         */
        private fun playingAlbumId(
            albums: AlbumCollection,
            nowPlaying: NowPlaying?,
        ): String? {
            if (nowPlaying == null || nowPlaying.isPaused) return null
            return albums.items.firstOrNull { it.name == nowPlaying.album }?.id
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_LOAD_ATTEMPTS = 3
            const val RETRY_DELAY_MS = 1_500L
        }
    }

private fun Album.toTile() = CollectionTile(id, name, spotifyImageUrl(cover?.url), ContentKind.Album)

private fun Artist.toTile() = CollectionTile(id, name, spotifyImageUrl(cover?.url), ContentKind.Artist)

private fun Playlist.toTile() = CollectionTile(id.value, name, spotifyImageUrl(cover?.url), ContentKind.Playlist)

private fun Album.toResume() =
    ResumeListening(
        title = name,
        subtitle =
            listOfNotNull(
                artistNames.takeIf { it.isNotBlank() },
                releaseYear?.toString(),
                trackCount.takeIf { it > 0 }?.let { "$it titres" },
            ).joinToString(" · "),
        // Un album n'est pas un titre : `isSaved` du SDK ne s'applique pas ici.
        isSaved = null,
    )
