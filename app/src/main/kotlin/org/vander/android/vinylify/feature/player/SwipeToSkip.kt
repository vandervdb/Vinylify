package org.vander.android.vinylify.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

private const val COMMIT_FRACTION = 0.25f
private const val SLIDE_OUT_MS = 120
private const val SLIDE_IN_MS = 180
private const val SNAP_BACK_MS = 150
private const val WAIT_NEW_TRACK_MS = 600L
private const val ENTER_FROM_FRACTION = 0.5f

/**
 * Sens attendu du prochain changement de titre, pour les changements qui ne viennent pas d'un
 * balayage (boutons, enchaînement automatique).
 *
 * Un bouton le renseigne juste avant d'envoyer sa commande ; [SwipeToSkip] le consomme quand le
 * titre change réellement. Un indice ancien est ignoré : « précédent » peut ne faire que
 * redémarrer le titre, sans changement, et ne doit pas orienter un enchaînement tardif.
 */
@Stable
class SkipAnimationState(
    private val nowMs: () -> Long = { System.nanoTime() / NANOS_PER_MS },
) {
    private var direction = 0f
    private var expectedAtMs = 0L

    fun expectNext() = expect(-1f)

    fun expectPrevious() = expect(1f)

    private fun expect(value: Float) {
        direction = value
        expectedAtMs = nowMs()
    }

    /** −1 = sortie vers la gauche (suivant), +1 = vers la droite (précédent), 0 = aucun indice valable. */
    internal fun consume(): Float {
        val fresh = direction != 0f && nowMs() - expectedAtMs <= HINT_VALIDITY_MS
        val result = if (fresh) direction else 0f
        direction = 0f
        return result
    }

    private companion object {
        const val HINT_VALIDITY_MS = 1_500L
        const val NANOS_PER_MS = 1_000_000L
    }
}

@Composable
fun rememberSkipAnimationState(): SkipAnimationState = remember { SkipAnimationState() }

/**
 * Version sans animation des changements extérieurs : le contenu lit toujours l'état courant.
 * C'est celle du MiniPlayer.
 */
@Composable
fun SwipeToSkip(
    trackKey: Any?,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeToSkip(
        trackKey = trackKey,
        item = Unit,
        onNext = onNext,
        onPrevious = onPrevious,
        modifier = modifier,
        animateExternalChanges = false,
    ) { content() }
}

/**
 * Fait glisser son [content] sous le doigt ; au relâchement, au-delà d'un quart de la largeur,
 * enchaîne sur le titre suivant (glissement vers la gauche) ou précédent (vers la droite).
 *
 * Le contenu sort du côté du geste, puis **attend que le titre ait vraiment changé** avant de
 * rentrer par le côté opposé : le SDK pousse le nouvel état de façon asynchrone, et faire rentrer
 * le contenu tout de suite montrerait l'ancien titre pendant une fraction de seconde. [trackKey]
 * identifie le titre courant pour détecter ce changement ; l'attente est bornée, car un saut
 * peut ne rien changer (début de file, titre qui redémarre).
 *
 * Quand le titre change **sans** balayage (bouton, enchaînement automatique) et que
 * [animateExternalChanges] est vrai, la même séquence est jouée : l'ancien contenu sort, puis le
 * nouveau entre. Pour cela l'ancien est gardé affiché : [content] reçoit [item] tant que le titre
 * affiché est le courant, et la dernière valeur de [item] de l'ancien titre pendant la sortie.
 * [skipAnimation] dit de quel côté sortir ; sans indice, vers la gauche, comme « suivant ».
 * [slideFraction], s'il est fourni, reçoit la part de course du glissement (0 à 1).
 *
 * Ce n'est pas un `HorizontalPager` : la file d'attente de la Web API est un instantané et ne
 * connaît pas le titre précédent, un pager n'aurait donc pas de page de gauche à montrer.
 */
@Composable
fun <T> SwipeToSkip(
    trackKey: Any?,
    item: T,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    skipAnimation: SkipAnimationState? = null,
    animateExternalChanges: Boolean = true,
    slideFraction: MutableFloatState? = null,
    content: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val currentKey by rememberUpdatedState(trackKey)
    val currentItem by rememberUpdatedState(item)
    // Le titre réellement affiché : il retarde sur [trackKey] le temps de la sortie de l'ancien.
    var shownKey by remember { mutableStateOf(trackKey) }
    var shownItem by remember { mutableStateOf(item) }
    var swiping by remember { mutableStateOf(false) }
    val face = if (!animateExternalChanges || shownKey == trackKey) item else shownItem
    val currentNext by rememberUpdatedState(onNext)
    val currentPrevious by rememberUpdatedState(onPrevious)

    suspend fun settle() {
        val direction = sign(offset.value)
        if (widthPx <= 0f || abs(offset.value) < widthPx * COMMIT_FRACTION) {
            offset.animateTo(0f, tween(SNAP_BACK_MS))
            return
        }
        val startKey = currentKey
        swiping = true
        try {
            offset.animateTo(direction * widthPx, tween(SLIDE_OUT_MS))
            if (direction < 0f) currentNext() else currentPrevious()
            withTimeoutOrNull(WAIT_NEW_TRACK_MS) { snapshotFlow { currentKey }.first { it != startKey } }
            shownKey = currentKey
            shownItem = currentItem
            offset.snapTo(-direction * widthPx * ENTER_FROM_FRACTION)
            offset.animateTo(0f, tween(SLIDE_IN_MS))
        } finally {
            swiping = false
        }
    }

    // Part de la course (0 = au repos, 1 = sorti) pour ce qui doit suivre le glissement sans en faire partie.
    LaunchedEffect(slideFraction) {
        val sink = slideFraction ?: return@LaunchedEffect
        snapshotFlow { abs(offset.value) / widthPx.coerceAtLeast(1f) }.collect { sink.floatValue = it.coerceIn(0f, 1f) }
    }

    // Changement de titre venu d'ailleurs que du doigt : on rejoue la même sortie puis entrée.
    LaunchedEffect(trackKey) {
        if (!animateExternalChanges || swiping || shownKey == trackKey) return@LaunchedEffect
        if (widthPx <= 0f) {
            shownKey = currentKey
            shownItem = currentItem
            return@LaunchedEffect
        }
        val direction = skipAnimation?.consume()?.takeIf { it != 0f } ?: -1f
        offset.stop()
        offset.animateTo(direction * widthPx, tween(SLIDE_OUT_MS))
        shownKey = currentKey
        shownItem = currentItem
        offset.snapTo(-direction * widthPx * ENTER_FROM_FRACTION)
        offset.animateTo(0f, tween(SLIDE_IN_MS))
    }

    Box(
        modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .clipToBounds()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { offset.stop() } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + delta) }
                    },
                    onDragEnd = { scope.launch { settle() } },
                    onDragCancel = { scope.launch { offset.animateTo(0f, tween(SNAP_BACK_MS)) } },
                )
            },
    ) {
        Box(
            Modifier.graphicsLayer {
                translationX = offset.value
                alpha = (1f - abs(offset.value) / widthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            },
        ) {
            content(face)
        }
    }
}
