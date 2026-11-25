package org.vander.android.vinylify.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vander.android.vinylify.feature.DetailState
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.model.PlaylistDetail
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.model.Track
import javax.inject.Inject

/** Détail d'une playlist. Même mécanique que `AlbumViewModel`. */
@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val playlistId = PlaylistId(checkNotNull(savedStateHandle[PlaylistRoute.ARG_ID]))

        private val _state = MutableStateFlow<DetailState<PlaylistDetail>>(DetailState.Loading)
        val state: StateFlow<DetailState<PlaylistDetail>> = _state.asStateFlow()

        init {
            load()
        }

        fun load() {
            _state.value = DetailState.Loading
            viewModelScope.launch {
                _state.value =
                    spotify.playlist(playlistId).fold(
                        onSuccess = { DetailState.Loaded(it) },
                        onFailure = { DetailState.Failed },
                    )
            }
        }

        fun playPlaylist() {
            val detail = (state.value as? DetailState.Loaded)?.value ?: return
            viewModelScope.launch { player.playPlaylist(detail.playlist) }
        }

        fun playTrack(track: Track) {
            viewModelScope.launch { player.play(track.id) }
        }
    }
