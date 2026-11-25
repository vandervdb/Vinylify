package org.vander.android.vinylify.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.modifier.drawFadeEdges
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

private const val START_PAUSE_MS = 1_200L

/**
 * Horizontal marquee for [text] that adapts to the space the parent offers:
 * if the text fits without truncation it is shown once, static, with no
 * animation; only when it overflows does it scroll continuously
 * right-to-left and loop seamlessly, with a fade-out gradient at both edges.
 *
 * How the adaptation works: the whole composable is wrapped in a
 * [BoxWithConstraints], whose `maxWidth` reflects the width the *parent*
 * actually offers (known in Dp at composition time — this is the standard
 * Compose way to react to incoming constraints, as opposed to reading them
 * inside a custom [Layout]'s measure lambda, which runs too late to gate
 * whether the scrolling `LaunchedEffect` should even start). That width is
 * compared, in pixels, against the text's own intrinsic width from
 * [rememberTextMeasurer] (measured unconstrained, so it reflects the text's
 * natural, un-wrapped width). Only when the text is wider than what the
 * parent offers do we compose the looping variant (two duplicated copies +
 * custom [Layout], see inline comment there).
 *
 * @param speedPxPerSecond scroll speed in raw pixels/second (not dp-scaled), used only while scrolling
 * @param spacer gap between the end of one loop and the start of the next, used only while scrolling
 * @param fadeEdgeWidth width of the edge fade-out gradient, used only while scrolling
 * @param fadeColor color the text fades into — should match the surface behind it
 * @param color text color; defaults to `onBackground`, which is only right on the plain background
 * @param enabled `false` freezes an overflowing text as a single truncated line instead of
 *   scrolling it. Defaults to [LocalMarqueeEnabled], which a scrolling list turns off while the
 *   finger moves it: dozens of rows scrolling sideways *and* vertically is unreadable and costs
 *   frames. The marquee starts again, from the beginning, once the list stops.
 */
@Composable
fun MarqueeTextInfinite(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    speedPxPerSecond: Float = 60f,
    spacer: Dp = 24.dp,
    fadeEdgeWidth: Dp = 16.dp,
    fadeColor: Color = MaterialTheme.colorScheme.surface,
    color: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = LocalMarqueeEnabled.current,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val measured =
        remember(text, textStyle) {
            textMeasurer.measure(text = text, style = textStyle)
        }

    val textWidthPx = measured.size.width
    val textHeightPx = measured.size.height
    val spacerPx = with(density) { spacer.toPx().roundToInt() }
    val totalLoopWidth = textWidthPx + spacerPx

    BoxWithConstraints(modifier = modifier) {
        val availableWidthPx = with(density) { maxWidth.toPx() }

        if (textWidthPx <= availableWidthPx) {
            BasicText(
                text = text,
                style = textStyle.copy(color = color),
                maxLines = 1,
                softWrap = false,
            )
            return@BoxWithConstraints
        }

        if (!enabled) {
            BasicText(
                text = text,
                style = textStyle.copy(color = color),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            return@BoxWithConstraints
        }

        val offsetX = remember { Animatable(0f) }

        LaunchedEffect(totalLoopWidth) {
            while (true) {
                offsetX.snapTo(0f)
                // Le début du texte reste lisible un instant avant de partir.
                delay(START_PAUSE_MS)
                offsetX.animateTo(
                    targetValue = -totalLoopWidth.toFloat(),
                    animationSpec =
                        tween(
                            durationMillis = ((totalLoopWidth / speedPxPerSecond) * 1000).toInt(),
                            easing = EaseInOutQuad,
                        ),
                )
            }
        }

        Layout(
            modifier =
                Modifier
                    .clipToBounds()
                    .drawFadeEdges(edgeWidth = fadeEdgeWidth, fadeColor = fadeColor),
            content = {
                BasicText(
                    text = text,
                    style = textStyle.copy(color = color),
                    maxLines = 1,
                    softWrap = false,
                )
                BasicText(
                    text = text,
                    style = textStyle.copy(color = color),
                    maxLines = 1,
                    softWrap = false,
                )
            },
        ) { measurables, constraints ->
            val safeMaxWidth = max(totalLoopWidth, constraints.minWidth)

            val placeables =
                measurables.map {
                    it.measure(constraints.copy(maxWidth = safeMaxWidth))
                }

            layout(width = constraints.maxWidth, height = textHeightPx) {
                // Two copies of the text placed back-to-back (second one shifted by
                // totalLoopWidth) and translated together by the same offset: once the
                // first copy has scrolled out by totalLoopWidth, the second copy sits
                // exactly where the first started, so the snapTo(0f) reset in
                // LaunchedEffect above is visually seamless — no gap or jump.
                val offset = offsetX.value.roundToInt()
                placeables[0].placeRelative(x = offset, y = 0)
                placeables[1].placeRelative(x = offset + totalLoopWidth, y = 0)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarqueeTextInfiniteShortPreview() {
    AndroidAppTheme {
        MarqueeTextInfinite(
            text = "Short text",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MarqueeTextInfiniteLongPreview() {
    AndroidAppTheme {
        MarqueeTextInfinite(
            text =
                "This is a very long text that will definitely overflow the screen " +
                    "and start the infinite marquee animation.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
