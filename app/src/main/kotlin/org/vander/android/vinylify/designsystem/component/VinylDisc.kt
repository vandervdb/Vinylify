package org.vander.android.vinylify.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.vander.android.vinylify.designsystem.GrooveFontFamily
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylDiscBase
import org.vander.android.vinylify.designsystem.VinylDiscDeep
import org.vander.android.vinylify.designsystem.VinylDiscGroove
import org.vander.android.vinylify.designsystem.VinylDiscGrooveHi
import org.vander.android.vinylify.designsystem.VinylDiscHole
import org.vander.android.vinylify.designsystem.VinylDiscMid
import org.vander.android.vinylify.designsystem.VinylDiscRim
import org.vander.android.vinylify.designsystem.VinylPurple
import org.vander.android.vinylify.designsystem.VinylPurpleDim
import org.vander.android.vinylify.designsystem.VinylTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Rayon de la ligne de base du texte, en fraction du rayon du disque. La bande
 *  0,304–0,400 du dégradé est lisse : le texte n'y croise aucun sillon. */
private const val GROOVE_TEXT_RADIUS = 0.35f

/**
 * Rayon du trou central, en fraction du rayon du disque (donc aussi son diamètre en fraction du
 * diamètre du disque). Cote unique : le trou du dégradé et la broche [DiscSpindle] en dépendent.
 */
const val DISC_HOLE_RADIUS = 0.04f

/** Part du trou occupée par la broche : le reste est le liseré sombre qui la détache du label. */
private const val SPINDLE_FILL = 0.8f

/** Corps du texte, en fraction du diamètre — comme toutes les cotes du composant. */
private const val GROOVE_TEXT_SIZE = 0.055f

private const val ELLIPSIS = "…"

/** Chasse ajoutée entre glyphes, en fraction du diamètre. */
private const val GROOVE_TEXT_TRACKING = 0.008f

/**
 * Disque vinyle : sillons concentriques + label accent.
 * Rendu 2D plat, aucun relief simulé — même vocabulaire que les maquettes.
 * @param labelColor couleur du label central (par défaut l'accent ; passer la couleur
 *                   dominante de la pochette pour les vignettes de collection).
 * @param grooveTextRadius rayon du texte, en fraction du rayon du disque. Les bandes lisses
 *                   (sans sillon) sont 0,30–0,40 (défaut), 0,42–0,52, 0,54–0,64, 0,66–0,76 et 0,78–0,88.
 * @param grooveTextAngleDegrees position du milieu du texte : −90° = en haut (défaut), 0° = à droite.
 * @param grooveTextMaxArcDegrees arc maximal occupé par le texte : au-delà, il est coupé au dernier mot
 *                   entier qui tient, suivi de « … ». Le corps ne change pas. Nul : pas de limite.
 * @param grooveText texte gravé le long d'un sillon, centré sur [grooveTextAngleDegrees] et lu dans le sens
 *                   horaire. Nul par défaut : à 28 dp (pastille de snackbar) il ne serait
 *                   qu'une bouillie de pixels.
 */
@Composable
fun VinylDisc(
    modifier: Modifier = Modifier,
    labelColor: Color = VinylPurple,
    grooveText: String? = null,
    grooveTextColor: Color = VinylTextSecondary,
    grooveTextRadius: Float = GROOVE_TEXT_RADIUS,
    grooveTextAngleDegrees: Float = -90f,
    grooveTextMaxArcDegrees: Float? = null,
) {
    // Un glyphe = une entrée de cache ; le défaut (8) serait saturé par un texte plus long.
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val baseStyle = MaterialTheme.typography.titleSmall
    // Stops en fraction du rayon, repris à l'identique de la maquette.
    val stops =
        arrayOf(
            0.000f to VinylDiscHole,
            DISC_HOLE_RADIUS to VinylDiscHole,
            DISC_HOLE_RADIUS + 0.002f to labelColor,
            0.200f to labelColor,
            0.204f to VinylPurpleDim,
            0.214f to VinylPurpleDim,
            0.220f to VinylDiscMid,
            0.280f to VinylDiscMid,
            0.284f to VinylDiscGroove,
            0.300f to VinylDiscGroove,
            0.304f to VinylDiscBase,
            0.400f to VinylDiscBase,
            0.404f to VinylDiscGroove,
            0.416f to VinylDiscGroove,
            0.420f to VinylDiscBase,
            0.520f to VinylDiscBase,
            0.524f to VinylDiscGrooveHi,
            0.536f to VinylDiscGrooveHi,
            0.540f to VinylDiscBase,
            0.640f to VinylDiscBase,
            0.644f to VinylDiscGroove,
            0.656f to VinylDiscGroove,
            0.660f to VinylDiscBase,
            0.760f to VinylDiscBase,
            0.764f to VinylDiscGrooveHi,
            0.776f to VinylDiscGrooveHi,
            0.780f to VinylDiscBase,
            0.880f to VinylDiscBase,
            0.884f to VinylDiscRim,
            0.900f to VinylDiscRim,
            0.904f to VinylDiscDeep,
            0.964f to VinylDiscDeep,
            0.968f to VinylDiscRim,
            1.000f to VinylDiscRim,
        )
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(*stops, center = c, radius = r), radius = r, center = c)
        // Reflet doux, fixe par rapport au disque (il tourne donc avec lui)
        drawCircle(
            Brush.sweepGradient(
                0.00f to Color.Transparent,
                0.11f to Color.White.copy(alpha = 0.16f),
                0.26f to Color.Transparent,
                0.56f to Color.Transparent,
                0.67f to Color.White.copy(alpha = 0.10f),
                0.81f to Color.Transparent,
                center = c,
            ),
            radius = r,
            center = c,
        )

        grooveText?.takeIf { it.isNotBlank() }?.let { text ->
            drawTextAlongArc(
                measurer = measurer,
                text = text,
                style =
                    baseStyle.copy(
                        fontFamily = GrooveFontFamily,
                        color = grooveTextColor,
                        fontSize = (size.minDimension * GROOVE_TEXT_SIZE).toSp(),
                    ),
                center = c,
                radius = r * grooveTextRadius,
                trackingPx = size.minDimension * GROOVE_TEXT_TRACKING,
                centerAngle = grooveTextAngleDegrees * PI.toFloat() / 180f,
                maxArc = grooveTextMaxArcDegrees?.let { it * PI.toFloat() / 180f },
            )
        }
    }
}

/**
 * Broche de plateau chromée, posée dans le trou du disque. Elle se dessine **à part et ne tourne pas** :
 * un reflet qui tournerait avec le disque contredirait la lumière, qui vient du haut à gauche.
 *
 * Sa taille est celle du trou : `fillMaxSize(DISC_HOLE_RADIUS)` dans un cadre de la taille du disque.
 */
@Composable
fun DiscSpindle(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        // Le trou : un liseré sombre autour de la broche.
        drawCircle(VinylDiscHole, radius = r, center = c)
        val pin = r * SPINDLE_FILL
        val light = Offset(c.x - pin * 0.35f, c.y - pin * 0.40f)
        drawCircle(
            Brush.radialGradient(
                0.00f to Color(0xFFFFFFFF),
                0.35f to Color(0xFFE6E9EE),
                0.70f to Color(0xFF8D929C),
                1.00f to Color(0xFF383C44),
                center = light,
                radius = pin * 1.6f,
            ),
            radius = pin,
            center = c,
        )
        // Reflet net, dans l'axe de la lumière.
        drawCircle(Color.White.copy(alpha = 0.9f), radius = pin * 0.22f, center = Offset(c.x - pin * 0.32f, c.y - pin * 0.36f))
    }
}

/**
 * Pose [text] glyphe par glyphe le long d'un cercle de rayon [radius], centré sur le haut
 * du disque et lu dans le sens horaire.
 *
 * Chaque glyphe est tourné de son propre angle + 90°, ce qui le laisse tangent au cercle,
 * tête vers l'extérieur. Au sommet l'angle vaut -90°, donc la rotation est nulle : le texte
 * est à l'endroit. C'est ce qui le rend lisible quand le disque est à l'arrêt, puisque le
 * loader au repos laisse [VinylDisc] à 0°.
 *
 * L'angle occupé par un glyphe est sa largeur divisée par le rayon — la définition même du
 * radian. Rien d'autre n'est nécessaire pour répartir un texte à chasse variable.
 */
private fun DrawScope.drawTextAlongArc(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    center: Offset,
    radius: Float,
    trackingPx: Float,
    centerAngle: Float,
    maxArc: Float? = null,
) {
    if (radius <= 0f) return

    // Angle occupé par chaque glyphe pour un corps et un texte donnés.
    fun glyphSteps(
        t: String,
        st: TextStyle,
        tracking: Float,
    ): Pair<List<TextLayoutResult>, List<Float>> {
        val l = t.map { measurer.measure(it.toString(), st) }
        return l to l.map { (it.size.width + tracking) / radius }
    }

    var (layouts, steps) = glyphSteps(text, style, trackingPx)
    if (maxArc != null && steps.sum() > maxArc) {
        // Corps inchangé : on coupe au dernier mot entier qui tient, puis « … ».
        var shown = text
        while (steps.sum() > maxArc && shown.length > 1) {
            val body = shown.removeSuffix(ELLIPSIS).trimEnd()
            val lastSpace = body.lastIndexOf(' ')
            // Un seul mot plus long que l'arc : à défaut de mot à retirer, on retire un caractère.
            val kept = if (lastSpace > 0) body.substring(0, lastSpace) else body.dropLast(1)
            shown = kept.trimEnd(' ', ',', ';', ':', '.', '-') + ELLIPSIS
            glyphSteps(shown, style, trackingPx).let { (l, st) -> layouts = l; steps = st }
        }
    }

    // Départ : le milieu voulu moins la moitié de l'arc total, pour centrer le texte dessus.
    var angle = centerAngle - steps.sum() / 2f

    layouts.forEachIndexed { i, layout ->
        angle += steps[i] / 2f
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        rotate(degrees = angle * 180f / PI.toFloat() + 90f, pivot = Offset(x, y)) {
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
            )
        }
        angle += steps[i] / 2f
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10)
@Composable
private fun VinylDiscPreview() {
    AndroidAppTheme {
        VinylDisc(
            modifier = Modifier.size(300.dp),
            grooveText = "VINYLIFY",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, name = "Custom Label")
@Composable
private fun VinylDiscCustomLabelPreview() {
    AndroidAppTheme {
        VinylDisc(
            modifier = Modifier.size(300.dp),
            labelColor = Color(0xFFE91E63), // Pinkish red
        )
    }
}
