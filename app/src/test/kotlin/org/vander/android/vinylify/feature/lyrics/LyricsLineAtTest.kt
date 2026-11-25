package org.vander.android.vinylify.feature.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.vander.spotify.model.LyricLine

class LyricsLineAtTest {
    private val synced =
        LyricsState.Synced(
            listOf(
                LyricLine(1_000, "un"),
                LyricLine(5_000, "deux"),
                LyricLine(9_000, ""),
                LyricLine(12_000, "trois"),
            ),
        )

    @Test
    fun `avant la premiere ligne, rien`() {
        assertNull(synced.lineAt(999))
    }

    @Test
    fun `la ligne dont l'horodatage est atteint`() {
        assertEquals("un", synced.lineAt(1_000))
        assertEquals("deux", synced.lineAt(6_000))
        assertEquals("trois", synced.lineAt(60_000))
    }

    @Test
    fun `une ligne vide (pause instrumentale) ne donne rien`() {
        assertNull(synced.lineAt(9_500))
    }

    @Test
    fun `sans paroles synchronisees, rien`() {
        assertNull(LyricsState.Loading.lineAt(5_000))
        assertNull(LyricsState.NotFound.lineAt(5_000))
        assertNull(LyricsState.Failed.lineAt(5_000))
        assertNull(LyricsState.Instrumental.lineAt(5_000))
        assertNull(LyricsState.Plain("du texte").lineAt(5_000))
    }
}
