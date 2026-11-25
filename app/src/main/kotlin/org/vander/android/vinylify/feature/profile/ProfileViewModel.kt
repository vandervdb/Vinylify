package org.vander.android.vinylify.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.vander.android.vinylify.util.spotifyImageUrl
import org.vander.spotify.SpotifyClient
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.User
import javax.inject.Inject

/**
 * @property name `null` tant que le profil n'est pas chargé.
 * @property isPremium `null` si l'offre est inconnue ; la mention « Premium » n'est alors pas affichée.
 * @property likedCount `null` tant que le total n'est pas connu : un « 0 » mentirait.
 */
data class ProfileUiState(
    val name: String? = null,
    val avatarUrl: String? = null,
    val isPremium: Boolean? = null,
    val albumCount: Int = 0,
    val likedCount: Int? = null,
    val followingCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
    ) : ViewModel() {
        private val user = MutableStateFlow<User?>(null)
        private val likedCount = MutableStateFlow<Int?>(null)

        val state: StateFlow<ProfileUiState> =
            combine(user, likedCount, spotify.savedAlbums, spotify.followedArtists) { user, liked, albums, artists ->
                ProfileUiState(
                    name = user?.displayName ?: user?.id?.value,
                    avatarUrl = spotifyImageUrl(user?.avatar?.url),
                    isPremium = user?.product?.let { user.isPremium },
                    albumCount = albums.total,
                    likedCount = liked,
                    followingCount = artists.total,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState())

        init {
            viewModelScope.launch {
                spotify.session.first { it is SessionState.Authorized }
                user.value = spotify.currentUser().getOrNull()
                likedCount.value = spotify.savedTracksTotal().getOrNull()
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
