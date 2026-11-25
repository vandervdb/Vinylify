package org.vander.android.vinylify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.feature.connection.SessionViewModel
import org.vander.android.vinylify.feature.connection.LaunchAnchor
import org.vander.android.vinylify.feature.connection.LocalLaunchAnchor
import org.vander.android.vinylify.feature.connection.SplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val session: SessionViewModel by viewModels()

    /** Vrai quand l'app a été composée et mise en page une première fois (voir `setContent`). */
    private var appReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Avant super.onCreate : le splash système reste affiché tant que la session n'est pas
        // restaurée, pour que la destination de départ soit connue à la première composition.
        // Il reste aussi tant que l'app n'est pas composée dessous : cette composition (Accueil, injection,
        // premiers chargements) prend plusieurs centaines de ms, et faite pendant l'animation elle la figerait.
        installSplashScreen().setKeepOnScreenCondition { session.restoring.value || !appReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidAppTheme {
                val restoring by session.restoring.collectAsStateWithLifecycle()
                if (!restoring) {
                    // Sauvegardé : une rotation ne doit pas rejouer un splash déjà terminé.
                    var splashDone by rememberSaveable { mutableStateOf(false) }
                    val anchor = remember { LaunchAnchor(active = !splashDone) }
                    LaunchedEffect(Unit) {
                        // Deux images : la composition et la première mise en page de l'app sont faites. Un délai
                        // borne l'attente, pour qu'un blocage ne garde jamais le splash système indéfiniment.
                        withTimeoutOrNull(APP_READY_TIMEOUT_MS) {
                            withFrameNanos { }
                            withFrameNanos { }
                        }
                        appReady = true
                    }
                    CompositionLocalProvider(LocalLaunchAnchor provides anchor) {
                        // L'app est composée sous le splash : sa première destination est prête quand il s'efface.
                        Box(Modifier.fillMaxSize()) {
                            // Composée dès le départ, dessinée une fois le splash en train de s'effacer (voir LaunchAnchor).
                            Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (anchor.revealed) 1f else 0f }) {
                                AppRoot(session)
                            }
                            // Le cycle ne démarre qu'une fois l'app prête : sa composition ne peut plus le figer.
                            if (!splashDone && appReady) {
                                SplashScreen(
                                    anchor = anchor,
                                    onFinished = {
                                        splashDone = true
                                        anchor.active = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val APP_READY_TIMEOUT_MS = 2_000L
    }
}
