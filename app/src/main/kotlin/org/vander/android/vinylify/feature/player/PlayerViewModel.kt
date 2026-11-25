package org.vander.android.vinylify.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.PlayerConnection
import org.vander.spotify.android.RepeatMode
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.auth.SessionState
import javax.inject.Inject

/**
 * Lecture en cours, partagée entre le MiniPlayer et l'écran Lecteur.
 *
 * Il doit être obtenu avec le **même** `ViewModelStoreOwner` aux deux endroits (l'entrée de
 * back stack de `MainGraph`) : deux instances ouvriraient deux fois la liaison App Remote.
 * Les deux axes de la lib restent séparés ici — la connexion de la liaison
 * ([connection]) est indépendante de l'identité ([SpotifyClient.session]).
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        val nowPlaying: StateFlow<NowPlaying?> = player.nowPlaying
        val connection: StateFlow<PlayerConnection> = player.connection

        /** `null` tant que l'état « liké » du titre courant n'est pas connu. */
        private val _isSaved = MutableStateFlow<Boolean?>(null)
        val isSaved: StateFlow<Boolean?> = _isSaved.asStateFlow()

        init {
            connectWhenSignedIn()
            trackSavedState()
        }

        fun toggleSave() {
            val track = nowPlaying.value?.track ?: return
            // État inconnu (lecture échouée ou pas encore revenue) : un tap relit au lieu de deviner.
            val target =
                _isSaved.value?.not() ?: run {
                    viewModelScope.launch { _isSaved.value = spotify.isSaved(track).getOrNull() }
                    return
                }
            viewModelScope.launch { spotify.setSaved(track, target).onSuccess { _isSaved.value = target } }
        }

        fun togglePlayPause() {
            val playing = nowPlaying.value ?: return
            viewModelScope.launch { if (playing.isPaused) player.resume() else player.pause() }
        }

        fun skipNext() {
            viewModelScope.launch { player.skipNext() }
        }

        fun skipPrevious() {
            viewModelScope.launch { player.skipPrevious() }
        }

        /**
         * « Titre précédent » tel que Spotify le fait : passé [RESTART_THRESHOLD_MS] de lecture, le
         * premier `skipPrevious` ne fait que **redémarrer** le titre courant. Pour un swipe qui
         * promet de changer de titre, on enchaîne donc un second appel dans ce cas.
         */
        fun previousTrack() {
            val playing = nowPlaying.value ?: return
            viewModelScope.launch {
                if (interpolatePosition(playing) > RESTART_THRESHOLD_MS) player.skipPrevious()
                player.skipPrevious()
            }
        }

        fun seekTo(positionMs: Long) {
            viewModelScope.launch { player.seekTo(positionMs) }
        }

        fun toggleShuffle() {
            val playing = nowPlaying.value ?: return
            viewModelScope.launch { player.setShuffle(!playing.shuffle) }
        }

        fun cycleRepeat() {
            val playing = nowPlaying.value ?: return
            val next =
                when (playing.repeat) {
                    RepeatMode.Off -> RepeatMode.Context
                    RepeatMode.Context -> RepeatMode.Track
                    RepeatMode.Track -> RepeatMode.Off
                }
            viewModelScope.launch { player.setRepeat(next) }
        }

        fun retryConnect() {
            viewModelScope.launch { player.connect() }
        }

        /** Relit l'état « liké » à chaque changement de titre, en annulant la lecture précédente. */
        private fun trackSavedState() {
            viewModelScope.launch {
                player.nowPlaying
                    .map { it?.track }
                    .distinctUntilChanged()
                    .collectLatest { track ->
                        _isSaved.value = null
                        if (track != null) _isSaved.value = spotify.isSaved(track).getOrNull()
                    }
            }
        }

        private fun connectWhenSignedIn() {
            viewModelScope.launch {
                spotify.session.first { it is SessionState.Authorized }
                player.connect()
            }
        }

        override fun onCleared() {
            player.disconnect()
        }
    

        private companion object {
            const val RESTART_THRESHOLD_MS = 3_000L
        }
    }
