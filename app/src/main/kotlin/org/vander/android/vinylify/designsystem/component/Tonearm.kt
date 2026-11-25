package org.vander.android.vinylify.designsystem.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.vander.android.vinylify.designsystem.AndroidAppTheme
import org.vander.android.vinylify.designsystem.VinylArmMetal
import org.vander.android.vinylify.designsystem.VinylArmMetalDim
import org.vander.android.vinylify.designsystem.VinylArmShadow
import org.vander.android.vinylify.designsystem.VinylInk
import org.vander.android.vinylify.designsystem.VinylPurple
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/** Course du bras entre repos et lecture, en ms. Même valeur que [VinylMiniTurntable]. */
private const val ARM_TRAVEL_MS = 600

/** Même courbe que les bras de [VinylLoader] et [VinylMiniTurntable] : les platines bougent pareil. */
private val ARM_EASING = CubicBezierEasing(0.34f, 0.06f, 0.2f, 1f)

/** Rayon du sillon où se pose l'aiguille au début du titre, en fraction du rayon du disque. */
private const val START_RADIUS = 0.93f

/** Rayon atteint en fin de titre. Pas plus près du centre : le sillon serait caché sous la pochette. */
private const val END_RADIUS = 0.52f

/**
 * Bras relevé : l'aiguille est juste au-delà du bord du disque. Plus loin, la cellule sortirait de
 * l'écran : le disque est calé à droite, à la marge près.
 */
private const val REST_RADIUS = 1.1f

/** Longueur pivot → aiguille, en rayons de disque. */
private const val ARM_LENGTH_RADII = 1.3f

/** Pivot : distance au bord droit du bloc, puis hauteur au-dessus du sommet du disque (fraction du côté de la pochette). */
private const val PIVOT_INSET = 0.03f
private const val PIVOT_RISE = 0.06f

/**
 * Point de la pointe de lecture : l'intersection du cercle de rayon [armLength] centré sur
 * [pivot] (le bras) et du cercle de rayon [needleRadius] centré sur [center] (le sillon).
 *
 * Deux solutions, de part et d'autre de la droite pivot → centre ; on prend la plus à droite,
 * celle qui retombe sur la partie visible du disque. Hors portée (aucune intersection), la
 * pointe est ramenée sur la droite pivot → centre plutôt que de planter.
 */
internal fun solveNeedle(
    pivot: Offset,
    center: Offset,
    armLength: Float,
    needleRadius: Float,
): Offset {
    val dx = center.x - pivot.x
    val dy = center.y - pivot.y
    val d = sqrt(dx * dx + dy * dy)
    if (d == 0f) return pivot
    val ux = dx / d
    val uy = dy / d
    // Distance, le long de pivot → centre, jusqu'au pied de la perpendiculaire menée à la pointe.
    val a = (armLength * armLength + d * d - needleRadius * needleRadius) / (2f * d)
    val h = sqrt((armLength * armLength - a * a).coerceAtLeast(0f))
    val foot = Offset(pivot.x + ux * a, pivot.y + uy * a)
    val first = Offset(foot.x - uy * h, foot.y + ux * h)
    val second = Offset(foot.x + uy * h, foot.y - ux * h)
    return if (first.x >= second.x) first else second
}

/**
 * Bras de platine posé par-dessus un disque qui dépasse d'une pochette (voir le Lecteur).
 *
 * La géométrie se déduit de la taille du bloc : [discOverhangRatio] donne le côté de la pochette
 * (`largeur / (1 + ratio)`), le disque est calé à droite et le surplus de hauteur au-dessus est
 * la place du pivot. Le composant ne dessine rien d'autre que le bras et son socle, et ne
 * capte aucun geste.
 *
 * - [playing] : l'aiguille repose sur le disque et avance vers l'intérieur avec [progress] ;
 *   à `false` le bras se relève et repart au repos, hors du disque, et l'ombre s'écarte pour
 *   donner la hauteur.
 * - [progress] : de 0 à 1, avancement du titre.
 */
@Composable
fun Tonearm(
    progress: Float,
    playing: Boolean,
    discOverhangRatio: Float,
    modifier: Modifier = Modifier,
) {
    val needleRadius by animateFloatAsState(
        targetValue = if (playing) START_RADIUS + (END_RADIUS - START_RADIUS) * progress.coerceIn(0f, 1f) else REST_RADIUS,
        animationSpec = tween(ARM_TRAVEL_MS, easing = ARM_EASING),
        label = "needleRadius",
    )
    val lift by animateFloatAsState(
        targetValue = if (playing) 0f else 1f,
        animationSpec = tween(ARM_TRAVEL_MS, easing = ARM_EASING),
        label = "lift",
    )

    Canvas(modifier) {
        val u = size.width / (1f + discOverhangRatio) // côté de la pochette = diamètre du disque
        val r = u / 2f
        val top = size.height - u
        val center = Offset(size.width - r, top + r)
        val pivot = Offset(size.width - PIVOT_INSET * u, top - PIVOT_RISE * u)
        val length = ARM_LENGTH_RADII * r
        val needle = solveNeedle(pivot, center, length, needleRadius * r)
        val angle = atan2(needle.y - pivot.y, needle.x - pivot.x) * 180f / PI.toFloat()

        // Ombre : proche de l'objet quand l'aiguille est posée, décalée quand le bras est levé.
        val shadow = Color.Black.copy(alpha = 0.38f - 0.16f * lift)
        translate(left = (0.012f + 0.03f * lift) * u, top = (0.02f + 0.05f * lift) * u) {
            drawArm(pivot, u, length, angle, flat = shadow)
            drawBase(pivot, u, flat = shadow)
        }
        drawArm(pivot, u, length, angle, flat = null)
        drawBase(pivot, u, flat = null)
    }
}

private fun fill(
    brush: Brush,
    flat: Color?,
): Brush = flat?.let { SolidColor(it) } ?: brush

/**
 * Le bras, dessiné à plat le long de +x depuis le pivot puis tourné de [angleDegrees].
 * [flat] non nul : silhouette unie, pour l'ombre.
 */
private fun DrawScope.drawArm(
    pivot: Offset,
    u: Float,
    length: Float,
    angleDegrees: Float,
    flat: Color?,
) {
    rotate(degrees = angleDegrees, pivot = pivot) {
        fun cylinder(
            fromX: Float,
            toX: Float,
            thickness: Float,
            top: Color,
            bottom: Color,
        ) {
            val y0 = pivot.y - thickness / 2f
            drawRoundRect(
                brush = fill(Brush.verticalGradient(listOf(top, bottom), startY = y0, endY = y0 + thickness), flat),
                topLeft = Offset(pivot.x + fromX, y0),
                size = Size(toX - fromX, thickness),
                cornerRadius = CornerRadius(thickness * 0.35f),
            )
        }
        // Contrepoids, derrière le pivot, puis tige qui le relie.
        cylinder(-0.07f * u, 0f, 0.02f * u, VinylArmMetal, VinylArmMetalDim)
        cylinder(-0.21f * u, -0.07f * u, 0.095f * u, VinylArmMetalDim, VinylArmShadow)
        // Tube principal.
        cylinder(0f, length - 0.07f * u, 0.024f * u, VinylArmMetal, VinylArmMetalDim)
        // Cellule : un corps plus épais, puis la pointe accent qui touche le sillon en (length, 0).
        cylinder(length - 0.085f * u, length - 0.02f * u, 0.05f * u, VinylArmMetal, VinylArmMetalDim)
        drawRoundRect(
            brush = fill(SolidColor(VinylPurple), flat),
            topLeft = Offset(pivot.x + length - 0.035f * u, pivot.y - 0.013f * u),
            size = Size(0.04f * u, 0.026f * u),
            cornerRadius = CornerRadius(0.008f * u),
        )
    }
}

/** Socle du pivot : embase, anneau, capuchon. Dessiné après le bras, donc par-dessus. */
private fun DrawScope.drawBase(
    pivot: Offset,
    u: Float,
    flat: Color?,
) {
    fun disc(
        radius: Float,
        top: Color,
        bottom: Color,
    ) = drawCircle(
        brush =
            fill(
                Brush.linearGradient(
                    listOf(top, bottom),
                    start = Offset(pivot.x - radius, pivot.y - radius),
                    end = Offset(pivot.x + radius, pivot.y + radius),
                ),
                flat,
            ),
        radius = radius,
        center = pivot,
    )
    disc(0.07f * u, VinylArmMetal, VinylArmMetalDim)
    disc(0.05f * u, VinylArmShadow, VinylArmMetalDim)
    disc(0.032f * u, VinylArmMetal, VinylArmMetalDim)
    if (flat == null) drawCircle(VinylInk, radius = 0.009f * u, center = pivot)
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, widthDp = 360)
@Composable
private fun TonearmPreview() {
    AndroidAppTheme {
        Tonearm(progress = 0.3f, playing = true, discOverhangRatio = 0.33f, modifier = Modifier.fillMaxWidth().height(300.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C0A10, widthDp = 360, name = "Relevé")
@Composable
private fun TonearmRestPreview() {
    AndroidAppTheme {
        Tonearm(progress = 0.3f, playing = false, discOverhangRatio = 0.33f, modifier = Modifier.fillMaxWidth().height(300.dp))
    }
}
