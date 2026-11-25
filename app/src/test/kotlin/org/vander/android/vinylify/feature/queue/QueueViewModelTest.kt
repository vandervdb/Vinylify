package org.vander.android.vinylify.feature.queue

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.RepeatMode
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.model.Queue
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class QueueViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()

    private fun playing(id: String) =
        NowPlaying(TrackId(id), "titre $id", "Artiste", "Album", null, 1_000, 0, false, false, RepeatMode.Off, 0)

    @Test
    fun `la file est relue a l'ouverture puis a chaque changement de titre`() =
        runTest {
            QueueViewModel(client, player)
            val afterOpen = client.calls.count { it == SpotifyCall.RefreshQueue }

            player.emitNowPlaying(playing("t1"))
            player.emitNowPlaying(playing("t2"))

            assertEquals(1, afterOpen)
            assertEquals(3, client.calls.count { it == SpotifyCall.RefreshQueue })
        }

    @Test
    fun `une file encore perimee est relue jusqu'a refleter le titre joue, puis on s'arrete`() =
        runTest {
            QueueViewModel(client, player)
            player.emitNowPlaying(playing("t2"))
            runCurrent()
            // L'API rend encore l'ancienne file : « en cours » n'est pas t2.
            client.emitQueue(Queue(currentlyPlaying = Track(TrackId("t1"), "titre t1")))
            val staleReads = client.calls.count { it == SpotifyCall.RefreshQueue }

            advanceTimeBy(1_100)
            runCurrent()
            assertEquals(staleReads + 1, client.calls.count { it == SpotifyCall.RefreshQueue })

            // L'API a rattrapé son retard : la lecture suivante est la dernière.
            client.emitQueue(Queue(currentlyPlaying = Track(TrackId("t2"), "titre t2")))
            advanceTimeBy(2_100)
            runCurrent()
            val afterCatchUp = client.calls.count { it == SpotifyCall.RefreshQueue }
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(staleReads + 2, afterCatchUp)
            assertEquals(afterCatchUp, client.calls.count { it == SpotifyCall.RefreshQueue })
        }

    @Test
    fun `une file qui ne rattrape jamais est relue un nombre borne de fois`() =
        runTest {
            QueueViewModel(client, player)
            player.emitNowPlaying(playing("t2"))
            runCurrent()

            advanceTimeBy(60_000)
            runCurrent()

            // 1 lecture à l'ouverture (titre inconnu) + 4 essais pour t2, pas un de plus.
            assertEquals(5, client.calls.count { it == SpotifyCall.RefreshQueue })
        }

    @Test
    fun `une simple mise a jour de position ne relit pas la file`() =
        runTest {
            QueueViewModel(client, player)
            player.emitNowPlaying(playing("t1"))
            val before = client.calls.count { it == SpotifyCall.RefreshQueue }

            player.emitNowPlaying(playing("t1").copy(positionMs = 500))

            assertTrue(client.calls.count { it == SpotifyCall.RefreshQueue } == before)
        }
}
