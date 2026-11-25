package org.vander.android.vinylify.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenArcTest {
    // Valeurs du Lecteur : disque dépassant de 33 %, texte au rayon 0,83, au plus 150°.
    private val arc = coverHiddenArc(overhangRatio = 0.33f, ringRadius = 0.83f, textArcDegrees = 150f)

    @Test
    fun `la fenetre est centree a gauche, la ou est la pochette`() {
        assertNotNull(arc)
        assertEquals(180f, arc!!.centerDegrees, 0f)
        // Arc caché 228° (360 - 2 * 65,8), moins la demi-longueur du texte : 114,2 - 75.
        assertEquals(39.2f, arc.halfWidthDegrees, 0.5f)
    }

    @Test
    fun `un texte trop long pour tenir derriere la pochette n'a pas de fenetre`() {
        assertNull(coverHiddenArc(overhangRatio = 0.33f, ringRadius = 0.83f, textArcDegrees = 240f))
    }

    @Test
    fun `texte a droite sur disque au repos, donc visible`() {
        assertFalse(isTextHidden(discAngle = 0f, textAngle = 0f, arc = arc!!))
    }

    @Test
    fun `disque tourne d'un demi-tour, donc le texte passe derriere la pochette`() {
        assertTrue(isTextHidden(discAngle = 180f, textAngle = 0f, arc = arc!!))
    }

    @Test
    fun `les bords de la fenetre`() {
        val half = arc!!.halfWidthDegrees
        assertTrue(isTextHidden(180f + half - 1f, 0f, arc))
        assertFalse(isTextHidden(180f + half + 1f, 0f, arc))
        assertTrue(isTextHidden(180f - half + 1f, 0f, arc))
        assertFalse(isTextHidden(180f - half - 1f, 0f, arc))
    }

    @Test
    fun `les angles hors de 0-360 sont ramenes au tour`() {
        assertTrue(isTextHidden(180f + 720f, 0f, arc!!))
        assertTrue(isTextHidden(-180f, 0f, arc))
        assertFalse(isTextHidden(360f * 3, 0f, arc))
    }
}
