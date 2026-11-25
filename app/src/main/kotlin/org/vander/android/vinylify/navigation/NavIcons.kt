package org.vander.android.vinylify.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icônes de la dock, à thème platine. Dessinées au trait (grille de 24) : la teinte vient de `Icon`,
 * et aucune n'a de variante « pleine », puisque l'onglet sélectionné n'affiche plus son icône.
 */
internal object NavIcons {
    /** Platine : un disque (sillon + axe) et le bras posé dessus. */
    val Turntable: ImageVector =
        strokeIcon("Turntable") {
            circle(10f, 14f, 7f)
            circle(10f, 14f, 1.4f)
            moveTo(20.5f, 3f)
            lineTo(15f, 10f)
        }

    /** Une loupe dont la lentille est un disque. */
    val RecordSearch: ImageVector =
        strokeIcon("RecordSearch") {
            circle(10f, 10f, 6.5f)
            circle(10f, 10f, 1.6f)
            moveTo(15f, 15f)
            lineTo(21f, 21f)
        }

    /** Pochettes empilées, la première laissant voir son disque. */
    val RecordSleeves: ImageVector =
        strokeIcon("RecordSleeves") {
            moveTo(6f, 4f)
            lineTo(18f, 4f)
            moveTo(4.5f, 7.5f)
            lineTo(19.5f, 7.5f)
            moveTo(3f, 11f)
            lineTo(21f, 11f)
            lineTo(21f, 21f)
            lineTo(3f, 21f)
            close()
            circle(12f, 16f, 3.2f)
        }

    /** Casque : on garde celui de Material, déjà au trait. */
    val Headphones: ImageVector = Icons.Outlined.Headphones
}

private fun strokeIcon(
    name: String,
    block: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

private fun PathBuilder.circle(
    cx: Float,
    cy: Float,
    r: Float,
) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * r, dy1 = 0f)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * r, dy1 = 0f)
    close()
}
