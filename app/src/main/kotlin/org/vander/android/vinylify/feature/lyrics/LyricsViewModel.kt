package org.vander.android.vinylify.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.vander.spotify.LyricsRepository
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.SpotifyPlayer
import javax.inject.Inject

/** Ce qui identifie les paroles d'un titre : la position change à chaque tick, pas elle. */
private data class LyricsQuery(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

/**
 * Paroles du titre en lecture. Elles suivent le titre : un changement de piste (swipe, fin du
 * titre) relance la recherche, et une réponse tardive de l'ancien titre est abandonnée
 * (`flatMapLatest`).
 */
@HiltViewModel
class LyricsViewModel
    @Inject
    constructor(
        player: SpotifyPlayer,
        private val repository: LyricsRepository,
    ) : ViewModel() {
        val nowPlaying: StateFlow<NowPlaying?> = player.nowPlaying

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<LyricsState> =
            player.nowPlaying
                .map { playing -> playing?.let { LyricsQuery(it.title, it.artist, it.album, it.durationMs) } }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    flow {
                        if (query == null) {
                            emit(LyricsState.NotFound)
                            return@flow
                        }
                        emit(LyricsState.Loading)
                        emit(
                            repository
                                .lyrics(query.title, query.artist, query.album.ifBlank { null }, query.durationMs)
                                .fold(onSuccess = { it.toState() }, onFailure = { LyricsState.Failed }),
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LyricsState.Loading)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
