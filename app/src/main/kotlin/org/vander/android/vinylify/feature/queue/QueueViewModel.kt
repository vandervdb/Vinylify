package org.vander.android.vinylify.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.model.Queue
import org.vander.spotify.model.TrackId
import javax.inject.Inject

/**
 * La file d'attente de la Web API est un instantané : il faut la redemander. On le fait à
 * l'ouverture, puis à chaque changement de titre poussé par l'App Remote, pour que l'écran ne
 * reste pas figé pendant qu'un disque se déroule.
 *
 * La Web API met un instant à refléter un changement de titre : lue aussitôt, elle rend encore
 * l'ancienne file (constaté sur le téléphone). On relit donc jusqu'à ce que « en cours » soit le
 * titre joué, avec un nombre d'essais borné.
 */
@HiltViewModel
class QueueViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
        player: SpotifyPlayer,
    ) : ViewModel() {
        val queue: StateFlow<Queue> = spotify.queue

        init {
            viewModelScope.launch {
                player.nowPlaying
                    .map { it?.track }
                    .distinctUntilChanged()
                    .collectLatest { track -> refreshUntilCurrent(track) }
            }
        }

        private suspend fun refreshUntilCurrent(track: TrackId?) {
            repeat(MAX_ATTEMPTS) { attempt ->
                val queue = spotify.refreshQueue().getOrNull()
                if (track == null || queue?.currentlyPlaying?.id == track) return
                delay(RETRY_STEP_MS * (attempt + 1))
            }
        }

        private companion object {
            const val MAX_ATTEMPTS = 4
            const val RETRY_STEP_MS = 1_000L
        }
    }
