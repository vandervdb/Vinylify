package org.vander.android.vinylify.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SkipAnimationStateTest {
    private var now = 0L
    private val subject = SkipAnimationState { now }

    @Test
    fun `suivant sort vers la gauche, precedent vers la droite`() {
        subject.expectNext()
        assertEquals(-1f, subject.consume(), 0f)

        subject.expectPrevious()
        assertEquals(1f, subject.consume(), 0f)
    }

    @Test
    fun `un indice ne sert qu'une fois`() {
        subject.expectNext()
        subject.consume()

        assertEquals(0f, subject.consume(), 0f)
    }

    @Test
    fun `sans indice, aucun sens n'est impose`() {
        assertEquals(0f, subject.consume(), 0f)
    }

    @Test
    fun `un indice ancien est ignore, comme un precedent qui n'a fait que redemarrer le titre`() {
        subject.expectPrevious()
        now += 2_000

        assertEquals(0f, subject.consume(), 0f)
    }
}
