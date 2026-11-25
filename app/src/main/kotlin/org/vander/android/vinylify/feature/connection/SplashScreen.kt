package org.vander.android.vinylify.feature.connection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import org.vander.android.vinylify.R
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.component.VinylLoader
import org.vander.android.vinylify.designsystem.modifier.drawGrainOverlay
import kotlin.math.roundToInt

/** Durée du cycle du loader : un cycle complet, donc il finit au repos, sans saut. */
const val SPLASH_DURATION_MS = 4000

/** Durée du glissement de la platine vers l'écran de connexion (ou du fondu vers l'Accueil). */
private const val MORPH_DURATION_MS = 600

/** Largeur de la platine du splash, en part de la largeur de l'écran. */
private const val SPLASH_LOADER_WIDTH_RATIO = 0.8f

/**
 * Pont entre le splash et l'écran qu'il recouvre.
 *
 * @property active vrai tant que le splash est à l'écran : la platine de [ConnectionScreen] reste
 *   alors invisible, c'est celle du splash qui joue son rôle.
 * @property loaderBounds position et taille (repère racine) de la platine de [ConnectionScreen], là
 *   où le splash doit l'amener. `null` si cet écran n'est pas affiché (session déjà autorisée). Simple
 *   `var` et non un état : le splash ne la lit qu'une fois, à la fin du cycle.
 */
@Stable
class LaunchAnchor(
    active: Boolean,
) {
    var active by mutableStateOf(active)

    /**
     * Vrai dès que le splash commence à s'effacer. L'app est composée sous le splash dès le départ (sa
     * première destination est prête et sa platine mesurée), mais elle n'est **dessinée** qu'à partir de là :
     * la dessiner dès le début doublerait le travail de chaque image, derrière un overlay opaque.
     */
    var revealed by mutableStateOf(!active)

    var loaderBounds: Rect? = null
}

/**
 * Fournit le [LaunchAnchor] à tout l'arbre sans le faire passer de paramètre en paramètre à travers
 * `NavHost` et `connectionNavGraph`. `null` par défaut : prévisualisations et tests de
 * [ConnectionScreen] fonctionnent sans splash.
 */
val LocalLaunchAnchor = staticCompositionLocalOf<LaunchAnchor?> { null }

/**
 * Splash commun à tous les démarrages, posé par-dessus l'app déjà composée. Le loader joue un cycle
 * de [SPLASH_DURATION_MS], puis :
 *  - si [ConnectionScreen] est dessous : la platine glisse et se redimensionne jusqu'à la sienne
 *    pendant que le fond s'efface, ce qui découvre halo, titre et bouton ;
 *  - sinon (session autorisée) : tout s'efface en fondu vers l'Accueil.
 *
 * Le splash système n'affiche que le fond (même couleur), car il ne peut pas animer de Compose.
 */
@Composable
fun SplashScreen(
    anchor: LaunchAnchor,
    onFinished: () -> Unit,
) {
    val finished by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    var target by remember { mutableStateOf<Rect?>(null) }
    var morphing by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                // Le splash recouvre des écrans déjà interactifs : il avale les touches.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
    ) {
        val density = LocalDensity.current
        val startSizePx = with(density) { (maxWidth * SPLASH_LOADER_WIDTH_RATIO).toPx() }
        val startTopLeft =
            Offset(
                (constraints.maxWidth - startSizePx) / 2f,
                (constraints.maxHeight - startSizePx) / 2f,
            )

        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION_MS.toLong())
            target = anchor.loaderBounds
            morphing = true
            anchor.revealed = true
            progress.animateTo(1f, tween(MORPH_DURATION_MS, easing = FastOutSlowInEasing))
            finished()
        }

        // `progress` n'est lu que dans les lambdas de calque : aucune recomposition pendant la transition,
        // et le loader garde sa taille de départ, seule une transformation le déplace et l'agrandit.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - progress.value }
                .background(MaterialTheme.colorScheme.background)
                .drawGrainOverlay(alpha = 0.82f),
        )
        VinylLoader(
            modifier =
                Modifier
                    .offset { IntOffset(startTopLeft.x.roundToInt(), startTopLeft.y.roundToInt()) }
                    .graphicsLayer {
                        val p = progress.value
                        val bounds = target
                        transformOrigin = TransformOrigin(0f, 0f)
                        if (bounds == null) {
                            // Vers l'Accueil il n'y a pas de platine d'arrivée : elle s'efface avec le fond.
                            alpha = 1f - p
                        } else {
                            val scale = lerp(startSizePx, bounds.width, p) / startSizePx
                            scaleX = scale
                            scaleY = scale
                            translationX = lerp(startTopLeft.x, bounds.left, p) - startTopLeft.x
                            translationY = lerp(startTopLeft.y, bounds.top, p) - startTopLeft.y
                        }
                    },
            size = with(density) { startSizePx.toDp() },
            // Le cycle est fini : on fige au repos pour que la platine ne reparte pas pendant la transition.
            animated = !morphing,
            cycleMs = SPLASH_DURATION_MS,
            grooveText = stringResource(R.string.loader_brand),
        )
    }
}

@Preview
@Composable
private fun PreviewSplash() {
    AndroidAppTheme { SplashScreen(anchor = LaunchAnchor(active = true), onFinished = {}) }
}
