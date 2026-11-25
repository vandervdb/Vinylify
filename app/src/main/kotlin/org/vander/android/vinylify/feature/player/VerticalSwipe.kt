package org.vander.android.vinylify.feature.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

private val COMMIT_DISTANCE = 72.dp

/**
 * Déclenche [onSwipeDown] ou [onSwipeUp] au relâchement d'un glissement vertical d'au moins
 * [COMMIT_DISTANCE]. Pas de suivi du doigt : l'animation est celle de la navigation.
 *
 * Un enfant qui consomme déjà le geste (liste défilante, glissement horizontal de la pochette)
 * passe avant ce détecteur : sur une liste, seul l'en-tête doit donc le porter.
 */
@Composable
fun Modifier.verticalSwipe(
    onSwipeDown: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
): Modifier {
    val currentDown by rememberUpdatedState(onSwipeDown)
    val currentUp by rememberUpdatedState(onSwipeUp)
    return pointerInput(Unit) {
        val threshold = COMMIT_DISTANCE.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onVerticalDrag = { change, delta ->
                change.consume()
                total += delta
            },
            onDragEnd = {
                if (total >= threshold) currentDown?.invoke() else if (total <= -threshold) currentUp?.invoke()
            },
        )
    }
}

/**
 * À poser sur une liste défilante : appelle [onDismiss] quand l'utilisateur tire vers le bas
 * **alors que la liste est déjà en butée haute**, d'au moins [COMMIT_DISTANCE], puis relâche.
 *
 * Seul le glissement que la liste n'a pas pu consommer compte : tant qu'elle peut encore défiler
 * vers le haut, rien ne s'accumule, et un défilement dans l'autre sens remet le cumul à zéro.
 */
@Composable
fun Modifier.pullDownToDismiss(onDismiss: () -> Unit): Modifier {
    val currentDismiss by rememberUpdatedState(onDismiss)
    val thresholdPx = with(LocalDensity.current) { COMMIT_DISTANCE.toPx() }
    val connection =
        remember(thresholdPx) {
            object : NestedScrollConnection {
                private var pulled = 0f

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        pulled = if (available.y > 0f) pulled + available.y else 0f
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pulled >= thresholdPx) currentDismiss()
                    pulled = 0f
                    return Velocity.Zero
                }
            }
        }
    return nestedScroll(connection)
}
