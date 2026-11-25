package org.vander.android.vinylify.feature.player

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylTextMuted
import org.vander.spotify.android.NowPlaying

internal const val TICK_MS = 250L

/**
 * Position de lecture actuelle. Le SDK ne pousse la position qu'aux changements d'état ; elle ne
 * s'écoule pas toute seule. On repart de `positionMs` au moment `emittedAtElapsedMs` et on
 * ajoute le temps écoulé tant que la lecture n'est pas en pause.
 */
fun interpolatePosition(nowPlaying: NowPlaying): Long {
    if (nowPlaying.isPaused) return nowPlaying.positionMs
    val elapsed = SystemClock.elapsedRealtime() - nowPlaying.emittedAtElapsedMs
    return (nowPlaying.positionMs + elapsed).coerceIn(0, nowPlaying.durationMs)
}

/** Position qui avance toute seule à l'écran, relancée à chaque nouvel état poussé. */
@Composable
fun rememberLivePosition(nowPlaying: NowPlaying): Long {
    var position by remember(nowPlaying) { mutableLongStateOf(interpolatePosition(nowPlaying)) }
    LaunchedEffect(nowPlaying) {
        if (nowPlaying.isPaused) return@LaunchedEffect
        while (true) {
            position = interpolatePosition(nowPlaying)
            delay(TICK_MS)
        }
    }
    return position
}

/**
 * Barre de progression fine et cliquable du MiniPlayer : un tap saute à l'endroit touché, un
 * glissement déplace la position et n'envoie le `seekTo` qu'au relâchement.
 *
 * Zone tactile plus haute que le trait dessiné : un trait de 3 dp ne se vise pas au doigt.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val duration = durationMs.coerceAtLeast(1)
    val fraction = if (dragFraction >= 0f) dragFraction else (positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (widthPx > 0) onSeek((offset.x / widthPx * duration).toLong().coerceIn(0, duration))
                }
            }.pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> if (widthPx > 0) dragFraction = (offset.x / widthPx).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ ->
                        if (widthPx > 0) dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) onSeek((dragFraction * duration).toLong())
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(VinylTextMuted.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(VinylPurple))
    }
}
