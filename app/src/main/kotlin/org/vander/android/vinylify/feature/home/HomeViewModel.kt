package org.vander.android.vinylify.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.auth.SessionState
import org.vander.android.vinylify.feature.ContentKind
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.model.PlaylistCollection
import org.vander.spotify.model.TrackId
import javax.inject.Inject

/**
 * Accueil : « Dernièrement écouté », regroupé par jour et par source, avec un héros qui relance
 * la dernière écoute.
 *
 * L'historique n'est pas un `StateFlow` de la lib (un appel, un résultat) : le ViewModel en garde
 * la dernière réponse. Les playlists, elles, viennent du cache de la lib et servent à nommer les
 * playlists de l'historique.
 *
 * @param nowMs horloge murale, injectable pour figer « Aujourd'hui » / « Hier » en test.
 */
@HiltViewModel
class HomeViewModel(
    private val spotify: SpotifyClient,
    private val player: SpotifyPlayer,
    private val nowMs: () -> Long,
) : ViewModel() {
    @Inject
    constructor(spotify: SpotifyClient, player: SpotifyPlayer) : this(spotify, player, System::currentTimeMillis)

    private val history = MutableStateFlow<List<PlayHistory>?>(null)

    private val loadFailed = MutableStateFlow(false)
    private var loadJob: Job? = null

    val state: StateFlow<HomeUiState> =
        combine(history, spotify.playlists, player.nowPlaying, loadFailed, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    init {
        refreshWhenSignedIn()
        refreshOnTrackChange()
    }

    /** Relance le chargement, à la demande de l'utilisateur après un échec. */
    fun onRetry() = refreshWhenSignedIn()

    fun onResumeListen() {
        val latest = state.value.sections.firstOrNull()?.entries?.firstOrNull() ?: return
        viewModelScope.launch {
            // Les `play*` ne lisent que l'URI : un nom vide suffit.
            when (latest.kind) {
                ContentKind.Album -> player.playAlbum(Album(id = latest.targetId, name = ""))
                ContentKind.Playlist -> player.playPlaylist(Playlist(id = PlaylistId(latest.targetId), name = ""))
                ContentKind.Artist -> player.playArtist(Artist(id = latest.targetId, name = ""))
            }
        }
    }

    /** Joue le titre seul : le SDK ne sait pas encore lancer un contexte à partir d'un titre donné. */
    fun onTrackClick(trackId: String) {
        viewModelScope.launch { player.play(TrackId(trackId)) }
    }

    /**
     * Attend l'autorisation avant de charger : la session peut être restaurée depuis le
     * DataStore après la création du ViewModel, et un appel sans jeton échouerait en
     * `NotSignedIn`.
     *
     * Un chargement qui échoue est retenté : juste après la connexion, le premier appel peut partir
     * avant que le jeton soit prêt. Un jeton émis avant l'ajout du scope `user-read-recently-played`
     * échoue en 403 à chaque essai : l'écran propose alors de réessayer, ce qui ne suffira qu'après
     * une reconnexion.
     */
    private fun refreshWhenSignedIn() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                loadFailed.value = false
                spotify.session.first { it is SessionState.Authorized }
                repeat(MAX_LOAD_ATTEMPTS) { attempt ->
                    if (loadRecent()) return@launch
                    if (attempt < MAX_LOAD_ATTEMPTS - 1) delay(RETRY_DELAY_MS * (attempt + 1))
                }
                loadFailed.value = true
            }
    }

    /**
     * Spotify n'inscrit un titre dans l'historique qu'une fois écouté : un changement de titre est
     * le moment où la liste a pu bouger. Le délai laisse le temps au serveur de l'enregistrer.
     */
    private fun refreshOnTrackChange() {
        viewModelScope.launch {
            player.nowPlaying
                .map { it?.track }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    delay(HISTORY_SETTLE_MS)
                    if (spotify.session.value is SessionState.Authorized) loadRecent()
                }
        }
    }

    /** Vrai si l'historique est chargé. Les playlists ne servent qu'à nommer : leur échec est toléré. */
    private suspend fun loadRecent(): Boolean {
        val recent = spotify.recentlyPlayed(HISTORY_LIMIT).getOrElse { return false }
        if (spotify.playlists.value.items.isEmpty()) spotify.refreshPlaylists()
        history.value = recent
        loadFailed.value = false
        return true
    }

    private fun buildState(
        history: List<PlayHistory>?,
        playlists: PlaylistCollection,
        nowPlaying: NowPlaying?,
        failed: Boolean,
    ): HomeUiState {
        val sections = groupRecent(history.orEmpty(), playlists.items, nowMs())
        val latest = sections.firstOrNull()?.entries?.firstOrNull()
        return HomeUiState(
            sections = sections,
            playingEntryId = playingEntryId(sections, nowPlaying),
            resume = latest?.let { ResumeListening(title = it.title, subtitle = it.detail, isSaved = null) },
            isLoaded = history != null,
            loadFailed = failed,
        )
    }

    /**
     * `NowPlaying` donne l'album du titre en cours, pas son contexte : on marque la première entrée
     * du même album. Approximation assumée tant que le SDK n'expose pas l'URI de contexte courante.
     */
    private fun playingEntryId(
        sections: List<RecentSection>,
        nowPlaying: NowPlaying?,
    ): String? {
        if (nowPlaying == null || nowPlaying.isPaused) return null
        val albumId = nowPlaying.albumId ?: return null
        return sections.asSequence().flatMap { it.entries }.firstOrNull { it.albumId == albumId }?.id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_LOAD_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_500L
        const val HISTORY_LIMIT = 50
        const val HISTORY_SETTLE_MS = 3_000L
    }
}
