package org.vander.android.vinylify.feature.home

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.vander.android.vinylify.data.history.PlayHistoryStore
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.SpotifyError
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.RepeatMode
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.Album
import org.vander.spotify.model.Artist
import org.vander.spotify.model.PlayContext
import org.vander.spotify.model.PlayContextType
import org.vander.spotify.model.PlayHistory
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()

    private val now = 1_795_860_000_000L

    private val nuits = Album(id = "a1", name = "Nuits blanches", artists = listOf(Artist("r1", "Elia Faure")))

    private fun play(
        id: String,
        minutesAgo: Long,
        album: Album = nuits,
    ) = PlayHistory(
        track = Track(id = TrackId(id), name = "Titre $id", artists = album.artists, album = album),
        playedAtMs = now - minutesAgo * 60_000,
        context = PlayContext(PlayContextType.Album, album.id),
    )

    private val store = InMemoryPlayHistoryStore()

    private fun viewModel() = HomeViewModel(client, player, store) { now }

    /** Même contrat que la table Room : clé (instant, titre), tri décroissant, purge par date. */
    private class InMemoryPlayHistoryStore : PlayHistoryStore {
        private val plays = MutableStateFlow<List<PlayHistory>>(emptyList())
        override val history: Flow<List<PlayHistory>> = plays

        override suspend fun merge(
            plays: List<PlayHistory>,
            keepAfterMs: Long,
        ) {
            this.plays.value =
                (this.plays.value + plays)
                    .distinctBy { it.playedAtMs to it.track.id }
                    .filter { it.playedAtMs >= keepAfterMs }
                    .sortedByDescending { it.playedAtMs }
        }
    }

    @Test
    fun `rien n'est charge tant que la session n'est pas autorisee`() =
        runTest {
            viewModel()

            assertTrue(client.calls.none { it is SpotifyCall.RecentlyPlayed })
        }

    @Test
    fun `une fois autorisee, les 50 dernieres ecoutes sont lues`() =
        runTest {
            viewModel()

            client.emitSession(SessionState.Authorized)

            assertTrue(SpotifyCall.RecentlyPlayed(50) in client.calls)
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `un chargement qui echoue est retente`() =
        runTest {
            client.nextFailure = SpotifyError.Network(RuntimeException("jeton pas prêt"))
            val subject = viewModel()

            client.emitSession(SessionState.Authorized)
            advanceUntilIdle()

            assertEquals(2, client.calls.count { it is SpotifyCall.RecentlyPlayed })
            subject.state.test {
                val state = expectMostRecentItem()
                assertEquals(false, state.loadFailed)
                assertEquals(true, state.isLoaded)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `trois echecs proposent de reessayer`() =
        runTest {
            val subject = viewModel()
            client.nextFailure = SpotifyError.Api(403, "scope manquant")
            client.emitSession(SessionState.Authorized)
            // Les essais partent à 0, 1,5 s puis 4,5 s : une panne injectée avant chacun.
            listOf(1_600L, 3_000L).forEach { step ->
                client.nextFailure = SpotifyError.Api(403, "scope manquant")
                advanceTimeBy(step)
            }
            advanceUntilIdle()

            assertEquals(3, client.calls.count { it is SpotifyCall.RecentlyPlayed })
            subject.state.test { assertEquals(true, expectMostRecentItem().loadFailed) }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `le heros reprend la derniere ecoute et Ecouter relance son album`() =
        runTest {
            client.playHistory = listOf(play("t1", minutesAgo = 5), play("t2", minutesAgo = 9))
            client.emitSession(SessionState.Authorized)
            val subject = viewModel()
            player.connect()
            advanceUntilIdle()

            subject.state.test {
                val state = expectMostRecentItem()
                assertEquals("Nuits blanches", state.resume?.title)
                assertEquals(RecentDay.Today, state.sections.single().day)
            }
            subject.onResumeListen()
            advanceUntilIdle()

            assertEquals(listOf<PlayerCommand>(PlayerCommand.PlayAlbum(Album(id = "a1", name = ""))), player.commands)
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `l'entree de l'album en lecture est marquee`() =
        runTest {
            client.playHistory = listOf(play("t1", minutesAgo = 5))
            client.emitSession(SessionState.Authorized)
            val subject = viewModel()
            advanceUntilIdle()

            player.emitNowPlaying(
                NowPlaying(
                    track = TrackId("t9"),
                    title = "Sous la neige",
                    artist = "Elia Faure",
                    album = "Nuits blanches",
                    coverImageUri = null,
                    durationMs = 268_000,
                    positionMs = 0,
                    isPaused = false,
                    shuffle = false,
                    repeat = RepeatMode.Off,
                    emittedAtElapsedMs = 0,
                    albumId = "a1",
                ),
            )

            subject.state.test {
                val state = expectMostRecentItem()
                assertEquals(state.sections.single().entries.single().id, state.playingEntryId)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `un tap sur un titre deplie le joue seul`() =
        runTest {
            client.emitSession(SessionState.Authorized)
            val subject = viewModel()
            player.connect()

            subject.onTrackClick("t2")
            advanceUntilIdle()

            assertEquals(listOf<PlayerCommand>(PlayerCommand.Play(TrackId("t2"))), player.commands)
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `l'historique s'accumule d'un chargement a l'autre`() =
        runTest {
            client.playHistory = listOf(play("t2", minutesAgo = 10), play("t1", minutesAgo = 20))
            client.emitSession(SessionState.Authorized)
            val subject = viewModel()
            advanceUntilIdle()

            // Spotify ne rend plus t1, sorti de sa fenêtre : l'app doit le garder.
            client.playHistory = listOf(play("t3", minutesAgo = 5), play("t2", minutesAgo = 10))
            subject.onRetry()
            advanceUntilIdle()

            subject.state.test {
                val state = expectMostRecentItem()
                assertEquals(3, state.sections.single().entries.single().playCount)
                assertEquals(now - 20 * 60_000, state.keptSinceMs)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `l'historique conserve s'affiche avant tout chargement`() =
        runTest {
            store.merge(listOf(play("t1", minutesAgo = 5)), keepAfterMs = 0)
            val subject = viewModel()

            subject.state.test {
                val state = expectMostRecentItem()
                assertTrue(state.isLoaded)
                assertEquals("Titre t1", state.sections.single().entries.single().title)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `les ecoutes de plus de 90 jours sont oubliees`() =
        runTest {
            client.playHistory = listOf(play("t1", minutesAgo = 5), play("t0", minutesAgo = 91L * 24 * 60))
            client.emitSession(SessionState.Authorized)
            val subject = viewModel()
            advanceUntilIdle()

            subject.state.test {
                assertEquals(listOf("Titre t1"), expectMostRecentItem().sections.flatMap { it.entries }.map { it.title })
            }
        }
}
