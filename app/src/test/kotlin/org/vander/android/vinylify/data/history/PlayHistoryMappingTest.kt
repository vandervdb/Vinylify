package org.vander.android.vinylify.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.Image
import org.vander.spotify.model.PlayContext
import org.vander.spotify.model.PlayContextType
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId

class PlayHistoryMappingTest {
    private val elia = Artist("r1", "Elia Faure")
    private val cleo = Artist("r2", "Cléo Wend")

    @Test
    fun `une ecoute survit a l'aller-retour par la table`() {
        val play =
            PlayHistory(
                track =
                    Track(
                        id = TrackId("t1"),
                        name = "Sous la neige",
                        artists = listOf(elia, cleo),
                        album =
                            Album(
                                id = "a1",
                                name = "Nuits blanches",
                                images = listOf(Image("https://i/a1")),
                                artists = listOf(elia),
                                trackCount = 10,
                            ),
                        durationMs = 268_000,
                    ),
                playedAtMs = 1_795_860_000_000L,
                context = PlayContext(PlayContextType.Playlist, "p1"),
            )

        assertEquals(play, play.toEntity().toDomain())
    }

    @Test
    fun `un type de contexte inconnu perd le contexte, pas l'ecoute`() {
        val entity =
            PlayHistory(track = Track(id = TrackId("t1"), name = "Seul"), playedAtMs = 1L)
                .toEntity()
                .copy(contextType = "Show", contextId = "s1")

        val play = entity.toDomain()

        assertEquals("Seul", play.track.name)
        assertNull(play.context)
        assertNull(play.track.album)
    }
}
