package org.vander.android.vinylify.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos

/** Un tour de disque, en ms. Maquette : `animation: spin 3.6s linear infinite`. */
private const val SPIN_CYCLE_MS = 3600

/** Durée de la décélération jusqu'à l'arrêt, quelle que soit la course restante. */
private const val SPIN_STOP_MS = 1400

/**
 * Course minimale laissée au disque pour ralentir. Sans elle, un arrêt déclenché juste
 * avant la fin d'un tour ne laisserait que quelques degrés, et la halte paraîtrait nette.
 */
private const val SPIN_STOP_MIN_SWEEP = 180f

private const val FULL_TURN = 360f

/**
 * Plage d'angles (repère du monde, 0° = à droite, sens horaire) où le **centre** d'un texte gravé le
 * laisse entièrement caché derrière la pochette : c'est là qu'on peut le changer sans que ça se voie.
 */
@Immutable
data class HiddenArc(
    val centerDegrees: Float,
    val halfWidthDegrees: Float,
)

/**
 * Plage de [HiddenArc] pour un disque de même diamètre que la pochette, qui la dépasse à droite de
 * [overhangRatio] (en diamètres), pour un texte d'au plus [textArcDegrees] gravé au rayon [ringRadius]
 * (en rayons du disque). `null` si le texte est trop long pour jamais tenir derrière la pochette.
 */
fun coverHiddenArc(
    overhangRatio: Float,
    ringRadius: Float,
    textArcDegrees: Float,
): HiddenArc? {
    // Bord droit de la pochette, mesuré depuis le centre du disque, en rayons du disque.
    val coverEdge = 1f - 2f * overhangRatio
    val visibleHalf = acos((coverEdge / ringRadius).coerceIn(-1f, 1f)) * 180f / PI.toFloat()
    val halfWidth = (180f - visibleHalf) - textArcDegrees / 2f
    return if (halfWidth < 0f) null else HiddenArc(centerDegrees = 180f, halfWidthDegrees = halfWidth)
}

/** Le centre du texte, gravé à [textAngle] sur un disque tourné de [discAngle], est-il dans [arc] ? */
fun isTextHidden(
    discAngle: Float,
    textAngle: Float,
    arc: HiddenArc,
): Boolean {
    val diff = ((discAngle + textAngle - arc.centerDegrees) % FULL_TURN + FULL_TURN + 180f) % FULL_TURN - 180f
    return abs(diff) <= arc.halfWidthDegrees
}

/**
 * Le disque, immobile ou en rotation.
 *
 * Même grammaire de mouvement que `VinylMiniTurntable` : un tour à la fois en repartant de
 * l'angle courant, et une décélération jusqu'à 0° à l'arrêt plutôt qu'un figeage net —
 * l'inertie d'un plateau. Pas de bras ici : la maquette n'en pose pas sur les vignettes.
 *
 * L'état de l'animation suit l'item par la clé de `items` (identifiant de la vignette), donc le recyclage
 * du `LazyVerticalGrid` ne le transfère pas d'une tile à l'autre.
 *
 * @param grooveTextMaxArcDegrees arc maximal du texte gravé ; au-delà il est coupé au dernier mot entier, suivi de « … ».
 * @param hiddenArc avec [swappedGrooveText] : plage où le texte est caché, seul moment où il change.
 * @param swappedGrooveText appelé une fois à chaque entrée du texte dans [hiddenArc] (donc au plus une
 *   fois par tour) ; son résultat remplace le texte gravé, `null` ramène à [grooveText]. Pas de minuteur :
 *   le texte n'est jamais relu hors de ces instants.
 * @param resetKey ramène le texte à [grooveText] quand il change, sans attendre le prochain swap.
 */
@Composable
fun SpinningDisc(
    spinning: Boolean,
    modifier: Modifier = Modifier,
    grooveText: String? = null,
    grooveTextRadius: Float = 0.35f,
    grooveTextAngleDegrees: Float = -90f,
    grooveTextMaxArcDegrees: Float? = null,
    hiddenArc: HiddenArc? = null,
    swappedGrooveText: (() -> String?)? = null,
    resetKey: Any? = null,
) {
    val angle = remember { Animatable(0f) }

    // Retombe sur [grooveText] quand il change (autre album) ou quand [resetKey] change (autre titre,
    // même album) : sinon la dernière ligne du titre précédent resterait gravée jusqu'au prochain swap.
    val shownText = remember(grooveText, resetKey) { mutableStateOf(grooveText) }
    val latestBase by rememberUpdatedState(grooveText)
    val latestSwapped by rememberUpdatedState(swappedGrooveText)
    if (hiddenArc != null && swappedGrooveText != null) {
        // [shownText] est une clé : quand il est recréé (reset), l'effet repart et écrit dans le nouvel état.
        // Sans elle, l'ancien effet continuerait d'écrire dans l'état abandonné et l'écran ne changerait plus.
        LaunchedEffect(hiddenArc, grooveTextAngleDegrees, shownText) {
            var wasHidden = false
            // Ne réagit qu'aux changements du booléen, pas à chaque image de la rotation.
            snapshotFlow { isTextHidden(angle.value, grooveTextAngleDegrees, hiddenArc) }.collect { hidden ->
                if (hidden && !wasHidden) shownText.value = latestSwapped?.invoke() ?: latestBase
                wasHidden = hidden
            }
        }
    }

    LaunchedEffect(spinning) {
        if (spinning) {
            // À l'arrêt l'effet est annulé et l'Animatable garde sa valeur : la décélération
            // enchaîne donc sans saut d'angle.
            while (isActive) {
                angle.animateTo(
                    targetValue = angle.value + FULL_TURN,
                    animationSpec = tween(SPIN_CYCLE_MS, easing = LinearEasing),
                )
                angle.snapTo(angle.value % FULL_TURN)
            }
            return@LaunchedEffect
        }

        if (angle.value == 0f) return@LaunchedEffect

        // On termine toujours sur 0°, quitte à ajouter un tour pour avoir de quoi ralentir.
        // LinearOutSlowIn part à pleine vitesse et s'éteint : c'est l'inertie d'un plateau,
        // pas une transition qui démarre.
        val remaining = FULL_TURN - angle.value
        val sweep = if (remaining < SPIN_STOP_MIN_SWEEP) remaining + FULL_TURN else remaining
        angle.animateTo(
            targetValue = angle.value + sweep,
            animationSpec = tween(SPIN_STOP_MS, easing = LinearOutSlowInEasing),
        )
        angle.snapTo(0f)
    }

    Box(modifier) {
        VinylDisc(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value },
            grooveText = shownText.value,
            grooveTextRadius = grooveTextRadius,
            grooveTextAngleDegrees = grooveTextAngleDegrees,
            grooveTextMaxArcDegrees = grooveTextMaxArcDegrees,
        )
        // Hors de la rotation : le reflet de la broche reste où est la lumière.
        DiscSpindle(Modifier.align(Alignment.Center).fillMaxSize(DISC_HOLE_RADIUS))
    }
}
