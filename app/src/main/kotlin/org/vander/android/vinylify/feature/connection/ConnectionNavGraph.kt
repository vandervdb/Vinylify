package org.vander.android.vinylify.feature.connection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.vander.spotify.auth.SessionState

@Serializable
object ConnectionRoute

/**
 * @param session le ViewModel de session vit à l'échelle de l'Activity et est passé ici : un
 *   `hiltViewModel()` dans la destination serait lié à son entrée de back stack et créerait
 *   une seconde instance.
 */
fun NavGraphBuilder.connectionNavGraph(
    session: SessionViewModel,
    onSessionReady: () -> Unit,
) {
    composable<ConnectionRoute> {
        val state by session.session.collectAsStateWithLifecycle()

        ConnectionScreen(
            state = state,
            onContinueWithSpotify = session::signIn,
            spinning = state is SessionState.Authorizing,
            // L'écran joue son cycle d'animation avant d'appeler : la navigation est à lui de déclencher.
            onConnected = onSessionReady,
        )
    }
}
