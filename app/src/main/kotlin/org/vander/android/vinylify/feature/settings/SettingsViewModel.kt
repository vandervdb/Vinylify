package org.vander.android.vinylify.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.vander.spotify.SpotifyClient
import org.vander.spotify.android.SpotifyPlayer
import org.vander.spotify.auth.SessionState
import javax.inject.Inject

/** @property userName `null` tant que le profil n'est pas chargé. */
data class SettingsUiState(
    val userName: String? = null,
)

/**
 * Réglages. La déconnexion met d'abord la lecture en pause, puis efface le jeton et ramène la session à `SignedOut` ; c'est `AppRoot`,
 * qui observe la session, qui renvoie ensuite vers l'écran de connexion — ce ViewModel ne navigue pas.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
        private val player: SpotifyPlayer,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                spotify.session.first { it is SessionState.Authorized }
                val user = spotify.currentUser().getOrNull()
                _state.value = SettingsUiState(userName = user?.displayName ?: user?.id?.value)
            }
        }

        fun signOut() {
            viewModelScope.launch {
                // D'abord : la déconnexion fait quitter MainGraph, ce qui ferme la liaison App Remote
                // (`PlayerViewModel.onCleared`) et rendrait la pause impossible. Sans liaison ouverte elle
                // échoue sans conséquence, et le délai évite qu'elle retarde la déconnexion.
                withTimeoutOrNull(PAUSE_TIMEOUT_MS) { player.pause() }
                spotify.signOut()
            }
        }

        private companion object {
            const val PAUSE_TIMEOUT_MS = 2_000L
        }
    }
