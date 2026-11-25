package org.vander.android.vinylify.feature.lyrics

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.LyricsRepository
import org.vander.spotify.SpotifyError
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.RepeatMode
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.model.LyricLine
import org.vander.spotify.model.Lyrics
import org.vander.spotify.model.TrackId

class LyricsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val player = FakeSpotifyPlayer()

    private companion object {
        const val LATENCY_MS = 100L
    }

    private class FakeLyrics(
        var result: Result<Lyrics?> = Result.success(null),
    ) : LyricsRepository {
        val queries = mutableListOf<String>()

        override suspend fun lyrics(
            track: String,
            artist: String,
            album: String?,
            durationMs: Long,
        ): Result<Lyrics?> {
            queries += "$artist|$track|$album|$durationMs"
            // Une vraie réponse met du temps : sans cela, `Loading` est fusionné par le StateFlow et invisible.
            delay(LATENCY_MS)
            return result
        }
    }

    private fun playing(
        title: String = "Medea",
        position: Long = 0,
        album: String = "Medea EP",
    ) = NowPlaying(
        track = TrackId("t1"),
        title = title,
        artist = "Damon Jee",
        album = album,
        coverImageUri = null,
        durationMs = 456_000,
        positionMs = position,
        isPaused = false,
        shuffle = false,
        repeat = RepeatMode.Off,
        emittedAtElapsedMs = 0,
    )

    @Test
    fun `des paroles synchronisees passent de Loading a Synced`() =
        runTest {
            val lines = listOf(LyricLine(1_000, "a"), LyricLine(2_000, "b"))
            val repository = FakeLyrics(Result.success(Lyrics(lines = lines, plain = "a\nb")))
            player.emitNowPlaying(playing())

            LyricsViewModel(player, repository).state.test {
                assertEquals(LyricsState.Loading, awaitItem())
                assertEquals(LyricsState.Synced(lines), awaitItem())
                expectNoEvents()
            }
            assertEquals(listOf("Damon Jee|Medea|Medea EP|456000"), repository.queries)
        }

    @Test
    fun `sans paroles, un echec et un titre instrumental ont chacun leur etat`() =
        runTest {
            val repository = FakeLyrics(Result.success(null))
            player.emitNowPlaying(playing())

            LyricsViewModel(player, repository).state.test {
                assertEquals(LyricsState.Loading, awaitItem())
                assertEquals(LyricsState.NotFound, awaitItem())

                repository.result = Result.failure(SpotifyError.Api(500, "boom"))
                player.emitNowPlaying(playing(title = "Autre"))
                assertEquals(LyricsState.Loading, awaitItem())
                assertEquals(LyricsState.Failed, awaitItem())

                repository.result = Result.success(Lyrics(instrumental = true))
                player.emitNowPlaying(playing(title = "Encore"))
                assertEquals(LyricsState.Loading, awaitItem())
                assertEquals(LyricsState.Instrumental, awaitItem())
            }
        }

    @Test
    fun `la progression de lecture ne relance pas la recherche, un autre titre oui`() =
        runTest {
            val repository = FakeLyrics(Result.success(Lyrics(plain = "texte")))
            val subject = LyricsViewModel(player, repository)
            subject.state.onEach { }.launchIn(backgroundScope)

            player.emitNowPlaying(playing(position = 0))
            advanceUntilIdle()
            player.emitNowPlaying(playing(position = 5_000))
            advanceUntilIdle()
            assertEquals(1, repository.queries.size)

            player.emitNowPlaying(playing(title = "Autre"))
            advanceUntilIdle()
            assertEquals(2, repository.queries.size)
            assertEquals(LyricsState.Plain("texte"), subject.state.value)
        }

    @Test
    fun `currentLineIndex rend la derniere ligne atteinte`() {
        val lines = listOf(LyricLine(1_000, "a"), LyricLine(5_000, "b"), LyricLine(9_000, "c"))

        assertEquals(-1, currentLineIndex(lines, 0))
        assertEquals(0, currentLineIndex(lines, 1_000))
        assertEquals(0, currentLineIndex(lines, 4_999))
        assertEquals(1, currentLineIndex(lines, 5_000))
        assertEquals(2, currentLineIndex(lines, 600_000))
        assertEquals(-1, currentLineIndex(emptyList(), 1_000))
    }
}
