package org.vander.android.vinylify.feature.album

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
import org.vander.spotify.model.AlbumDetail
import org.vander.spotify.model.Track
import javax.inject.Inject

/**
 * Détail d'un album (maquette écran 05). L'identifiant vient des arguments de la route, que
 * Navigation recopie dans le `SavedStateHandle` sous le nom de la propriété (`id`).
 */
@HiltViewModel
class AlbumViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val albumId: String = checkNotNull(savedStateHandle[AlbumRoute.ARG_ID])

        private val _state = MutableStateFlow<DetailState<AlbumDetail>>(DetailState.Loading)
        val state: StateFlow<DetailState<AlbumDetail>> = _state.asStateFlow()

        init {
            load()
        }

        fun load() {
            _state.value = DetailState.Loading
            viewModelScope.launch {
                _state.value =
                    spotify.album(albumId).fold(
                        onSuccess = { DetailState.Loaded(it) },
                        onFailure = { DetailState.Failed },
                    )
            }
        }

        fun playAlbum() {
            val detail = (state.value as? DetailState.Loaded)?.value ?: return
            viewModelScope.launch { player.playAlbum(detail.album) }
        }

        fun playTrack(track: Track) {
            viewModelScope.launch { player.play(track.id) }
        }
    }
