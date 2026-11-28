package org.vander.android.vinylify.feature.home

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.vander.android.vinylify.feature.ContentKind
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.PlayContext
import org.vander.spotify.model.PlayContextType
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId

class RecentGroupingTest {
    // 2026-11-28 12:00 UTC
    private val now = 1_795_867_200_000L
    private val hour = 3_600_000L

    private val elia = Artist("r1", "Elia Faure")
    private val nuits = Album(id = "a1", name = "Nuits blanches", artists = listOf(elia))
    private val maree = Album(id = "a2", name = "Marée haute", artists = listOf(elia))
    private val sillons = Playlist(id = PlaylistId("p1"), name = "Sillons")

    private fun play(
        id: String,
        hoursAgo: Long,
        album: Album = nuits,
        context: PlayContext? = PlayContext(PlayContextType.Album, album.id),
    ) = PlayHistory(
        track = Track(id = TrackId(id), name = "Titre $id", artists = album.artists, album = album),
        playedAtMs = now - hoursAgo * hour,
        context = context,
    )

    private fun group(
        vararg plays: PlayHistory,
        playlists: List<Playlist> = emptyList(),
    ) = groupRecent(plays.toList(), playlists, now, ZoneOffset.UTC)

    @Test
    fun `les ecoutes consecutives d'un meme album forment une seule ligne`() {
        val sections = group(play("t1", 1), play("t2", 1), play("t3", 2), play("t4", 3, album = maree), play("t5", 4))

        val entries = sections.single().entries
        assertEquals(listOf("Nuits blanches", "Titre t4", "Titre t5"), entries.map { it.title })
        assertEquals(listOf(3, 1, 1), entries.map { it.playCount })
        assertEquals("Elia Faure", entries[0].detail)
        assertEquals("Elia Faure · Marée haute", entries[1].detail)
    }

    @Test
    fun `les sections suivent le jour civil`() {
        val sections = group(play("t1", 1), play("t2", 14, album = maree), play("t3", 60), play("t4", 24 * 9))

        assertEquals(listOf(RecentDay.Today, RecentDay.Yesterday, RecentDay.ThisWeek, RecentDay.Earlier), sections.map { it.day })
    }

    @Test
    fun `un groupe a cheval sur minuit est coupe en deux`() {
        val sections = group(play("t1", 11), play("t2", 13))

        assertEquals(listOf(1, 1), sections.map { it.entries.single().playCount })
    }

    @Test
    fun `une playlist connue prend son nom, sinon le titre sert de libelle`() {
        val known = PlayContext(PlayContextType.Playlist, "p1")
        val unknown = PlayContext(PlayContextType.Playlist, "p2")

        val entries =
            group(
                play("t1", 1, context = known),
                play("t2", 1, album = maree, context = known),
                play("t3", 2, context = unknown),
                play("t4", 2, context = unknown),
                playlists = listOf(sillons),
            ).single().entries

        assertEquals(listOf("Sillons", "Titre t3"), entries.map { it.title })
        assertEquals(listOf(ContentKind.Playlist, ContentKind.Playlist), entries.map { it.kind })
        assertEquals(listOf("p1", "p2"), entries.map { it.targetId })
    }

    @Test
    fun `sans contexte on ouvre l'album du titre`() {
        val entry = group(play("t1", 1, context = null), play("t2", 1, context = null)).single().entries.single()

        assertEquals(ContentKind.Album, entry.kind)
        assertEquals("a1", entry.targetId)
        assertEquals(2, entry.playCount)
    }

    @Test
    fun `un groupe d'album porte sa duree, sa plage, sa progression et ses titres`() {
        val album = nuits.copy(trackCount = 10)
        fun timed(id: String, hoursAgo: Long) =
            play(id, hoursAgo, album = album).let { it.copy(track = it.track.copy(durationMs = 240_000)) }

        val entry = group(timed("t3", 1), timed("t2", 2), timed("t3", 3)).single().entries.single()

        assertEquals(720_000L, entry.totalDurationMs)
        assertEquals(now - 3 * hour, entry.firstPlayedAtMs)
        assertEquals(now - hour, entry.playedAtMs)
        assertEquals(AlbumProgress(heard = 2, total = 10), entry.albumProgress)
        assertEquals(listOf("t3", "t2", "t3"), entry.tracks.map { it.id })
    }

    @Test
    fun `pas de progression pour une playlist, un single ou une ecoute seule`() {
        val single = Album(id = "s1", name = "Single", artists = listOf(elia), trackCount = 1)
        val ten = nuits.copy(trackCount = 10)
        val inPlaylist = PlayContext(PlayContextType.Playlist, "p1")

        val entries =
            group(
                play("t1", 1, album = ten, context = inPlaylist),
                play("t2", 1, album = ten, context = inPlaylist),
                play("t3", 2, album = single),
                play("t4", 2, album = single),
                play("t5", 3, album = ten),
            ).single().entries

        assertEquals(listOf(null, null, null), entries.map { it.albumProgress })
    }
}
