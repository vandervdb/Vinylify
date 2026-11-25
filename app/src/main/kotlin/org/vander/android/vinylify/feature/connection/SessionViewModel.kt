package org.vander.android.vinylify.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vander.spotify.SpotifyClient
import org.vander.spotify.auth.SessionState
import javax.inject.Inject

/**
 * Identité de l'utilisateur, à l'échelle de l'Activity.
 *
 * Au démarrage, [SpotifyClient.restoreSession] reprend un éventuel jeton stocké : sans elle,
 * `session` vaut `SignedOut` jusqu'au premier appel API et l'écran de connexion
 * réapparaîtrait à chaque lancement. [restoring] tient le splash tant que la réponse n'est
 * pas connue.
 *
 * L'App Remote n'est pas ici : c'est un autre axe, porté par `PlayerViewModel`.
 */
@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        private val spotify: SpotifyClient,
    ) : ViewModel() {
        val session: StateFlow<SessionState> = spotify.session

        private val _restoring = MutableStateFlow(true)
        val restoring: StateFlow<Boolean> = _restoring.asStateFlow()

        init {
            viewModelScope.launch {
                // L'échec (pas de jeton, refresh refusé) n'est pas une erreur à afficher : on
                // retombe simplement sur l'écran de connexion.
                spotify.restoreSession()
                _restoring.value = false
            }
        }

        fun signIn() {
            viewModelScope.launch { spotify.signIn() }
        }
    }
