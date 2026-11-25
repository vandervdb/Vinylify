package org.vander.android.vinylify.designsystem.component

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonearmGeometryTest {
    // Côté de pochette 300, disque calé à droite : centre (~(249, 200)), pivot en haut à droite.
    private val pivot = Offset(391f, 82f)
    private val center = Offset(249f, 250f)
    private val armLength = 195f

    private fun dist(
        a: Offset,
        b: Offset,
    ) = (a - b).getDistance()

    @Test
    fun `la pointe est a la longueur du bras du pivot et au rayon voulu du centre`() {
        listOf(75f, 120f, 180f).forEach { radius ->
            val needle = solveNeedle(pivot, center, armLength, radius)

            assertEquals(armLength, dist(pivot, needle), 0.5f)
            assertEquals(radius, dist(center, needle), 0.5f)
        }
    }

    @Test
    fun `parmi les deux solutions, la pointe retombe a droite du centre`() {
        val needle = solveNeedle(pivot, center, armLength, 120f)

        assertTrue(needle.x > center.x)
    }

    @Test
    fun `plus la pointe est pres du centre, plus le bras pivote vers l'interieur`() {
        val outer = solveNeedle(pivot, center, armLength, 180f)
        val inner = solveNeedle(pivot, center, armLength, 90f)

        assertTrue(inner.x < outer.x)
    }

    @Test
    fun `hors de portee, la pointe reste finie`() {
        val needle = solveNeedle(pivot, center, armLength, 5_000f)

        assertTrue(needle.x.isFinite() && needle.y.isFinite())
    }
}
