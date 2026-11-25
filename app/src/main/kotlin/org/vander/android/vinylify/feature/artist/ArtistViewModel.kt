package org.vander.android.vinylify.feature.artist

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
import org.vander.spotify.model.ArtistDetail
import org.vander.spotify.model.Track
import javax.inject.Inject

/** Détail d'un artiste (maquette écran 08). Même mécanique que `AlbumViewModel`. */
@HiltViewModel
class ArtistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val artistId: String = checkNotNull(savedStateHandle[ArtistRoute.ARG_ID])

        private val _state = MutableStateFlow<DetailState<ArtistDetail>>(DetailState.Loading)
        val state: StateFlow<DetailState<ArtistDetail>> = _state.asStateFlow()

        /** `null` tant que l'état « suivi » n'est pas connu : le bouton n'est pas affiché. */
        private val _isFollowing = MutableStateFlow<Boolean?>(null)
        val isFollowing: StateFlow<Boolean?> = _isFollowing.asStateFlow()

        /** Vrai quand Spotify a refusé de modifier le suivi (droit manquant, réseau). */
        private val _followFailed = MutableStateFlow(false)
        val followFailed: StateFlow<Boolean> = _followFailed.asStateFlow()

        init {
            load()
            viewModelScope.launch { _isFollowing.value = spotify.isFollowingArtist(artistId).getOrNull() }
        }

        fun toggleFollow() {
            val target = _isFollowing.value?.not() ?: return
            viewModelScope.launch {
                _followFailed.value = false
                spotify.setFollowingArtist(artistId, target).fold(
                    onSuccess = {
                        _isFollowing.value = target
                        // Accueil, Bibliothèque et Profil lisent `followedArtists` : on les met à jour.
                        spotify.refreshFollowedArtists()
                    },
                    onFailure = { _followFailed.value = true },
                )
            }
        }

        fun load() {
            _state.value = DetailState.Loading
            viewModelScope.launch {
                _state.value =
                    spotify.artist(artistId).fold(
                        onSuccess = { DetailState.Loaded(it) },
                        onFailure = { DetailState.Failed },
                    )
            }
        }

        fun playArtist() {
            val detail = (state.value as? DetailState.Loaded)?.value ?: return
            viewModelScope.launch { player.playArtist(detail.artist) }
        }

        fun playTrack(track: Track) {
            viewModelScope.launch { player.play(track.id) }
        }
    }
