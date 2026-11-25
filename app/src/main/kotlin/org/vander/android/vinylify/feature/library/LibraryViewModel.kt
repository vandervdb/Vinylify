package org.vander.android.vinylify.feature.library

import org.vander.android.vinylify.feature.ContentKind
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vander.android.vinylify.feature.home.HomeFilter
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.SpotifyClient
import org.vander.spotify.auth.SessionState
import javax.inject.Inject

/**
 * @property detail artistes d'un album ; `null` pour les autres genres.
 * @property trackCount nombre de titres d'une playlist ; `0` pour les autres genres.
 */
data class LibraryRow(
    val id: String,
    val kind: ContentKind,
    val title: String,
    val coverUrl: String?,
    val detail: String? = null,
    val trackCount: Int = 0,
)

/**
 * @property likedCount `null` tant que le total n'est pas connu : la ligne « Titres likés »
 *   n'est alors pas dessinée plutôt que d'afficher un faux « 0 titre ».
 */
data class LibraryUiState(
    val rows: List<LibraryRow> = emptyList(),
    val likedCount: Int? = null,
    val selectedFilter: HomeFilter = HomeFilter.All,
)

/**
 * Bibliothèque (maquette écran 04) : les mêmes données que l'Accueil, en liste.
 *
 * Les `StateFlow` de la lib sont déjà chargés par l'Accueil, onglet de départ : ce ViewModel ne
 * recharge que ce qu'il est seul à demander, le total des titres likés.
 */
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
    ) : ViewModel() {
        private val filter = MutableStateFlow(HomeFilter.All)
        private val likedCount = MutableStateFlow<Int?>(null)

        val state: StateFlow<LibraryUiState> =
            combine(
                spotify.savedAlbums,
                spotify.playlists,
                spotify.followedArtists,
                filter,
                likedCount,
            ) { albums, playlists, artists, selected, liked ->
                val albumRows =
                    albums.items.map {
                        LibraryRow(it.id, ContentKind.Album, it.name, spotifyImageUrl(it.cover?.url), detail = it.artistNames)
                    }
                val playlistRows =
                    playlists.items.map {
                        LibraryRow(it.id.value, ContentKind.Playlist, it.name, spotifyImageUrl(it.cover?.url), trackCount = it.trackCount)
                    }
                val artistRows =
                    artists.items.map { LibraryRow(it.id, ContentKind.Artist, it.name, spotifyImageUrl(it.cover?.url)) }
                LibraryUiState(
                    rows =
                        when (selected) {
                            HomeFilter.All -> albumRows + playlistRows + artistRows
                            HomeFilter.Albums -> albumRows
                            HomeFilter.Playlists -> playlistRows
                            HomeFilter.Artists -> artistRows
                        },
                    likedCount = liked,
                    selectedFilter = selected,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

        init {
            viewModelScope.launch {
                spotify.session.first { it is SessionState.Authorized }
                likedCount.value = spotify.savedTracksTotal().getOrNull()
            }
        }

        fun onFilterSelect(value: HomeFilter) = filter.update { value }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
