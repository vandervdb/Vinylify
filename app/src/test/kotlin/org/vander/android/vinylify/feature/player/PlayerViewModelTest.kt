package org.vander.android.vinylify.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.NowPlaying
import org.vander.spotify.android.PlayerConnection
import org.vander.spotify.android.RepeatMode
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient

class PlayerViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()

    private fun nowPlaying(
        paused: Boolean = false,
        shuffle: Boolean = false,
        repeat: RepeatMode = RepeatMode.Off,
    ) = NowPlaying(
        track = TrackId("t1"),
        title = "Sous la neige",
        artist = "Elia Faure",
        album = "Nuits blanches",
        coverImageUri = null,
        durationMs = 268_000,
        positionMs = 0,
        isPaused = paused,
        shuffle = shuffle,
        repeat = repeat,
        emittedAtElapsedMs = 0,
    )

    private fun viewModel() = PlayerViewModel(client, player)

    @Test
    fun `la liaison App Remote s'ouvre quand la session est autorisee, pas avant`() {
        viewModel()
        assertEquals(PlayerConnection.NotConnected, player.connection.value)

        client.emitSession(SessionState.Authorized)

        assertEquals(PlayerConnection.Connected, player.connection.value)
    }

    @Test
    fun `togglePlayPause met en pause un titre qui joue`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)
        player.emitNowPlaying(nowPlaying(paused = false))

        subject.togglePlayPause()

        assertEquals(listOf<PlayerCommand>(PlayerCommand.Pause), player.commands)
    }

    @Test
    fun `togglePlayPause reprend un titre en pause`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)
        player.emitNowPlaying(nowPlaying(paused = true))

        subject.togglePlayPause()

        assertEquals(listOf<PlayerCommand>(PlayerCommand.Resume), player.commands)
    }

    @Test
    fun `cycleRepeat enchaine Off, Context, Track, Off`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)

        listOf(RepeatMode.Off, RepeatMode.Context, RepeatMode.Track).forEach { current ->
            player.emitNowPlaying(nowPlaying(repeat = current))
            subject.cycleRepeat()
        }

        assertEquals(
            listOf<PlayerCommand>(
                PlayerCommand.SetRepeat(RepeatMode.Context),
                PlayerCommand.SetRepeat(RepeatMode.Track),
                PlayerCommand.SetRepeat(RepeatMode.Off),
            ),
            player.commands,
        )
    }

    @Test
    fun `sans lecture les commandes ne font rien`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)

        subject.togglePlayPause()
        subject.toggleShuffle()

        assertEquals(emptyList<PlayerCommand>(), player.commands)
        assertNull(subject.isSaved.value)
    }

    @Test
    fun `l'etat like du titre est relu puis bascule par toggleSave`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)

        player.emitNowPlaying(nowPlaying())
        assertEquals(false, subject.isSaved.value)

        subject.toggleSave()

        assertEquals(true, subject.isSaved.value)
    }

    @Test
    fun `toggleSave a l'etat inconnu relit l'etat au lieu de deviner`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)
        // La lecture de l'etat du titre echoue : l'etat reste inconnu.
        client.nextFailure = org.vander.spotify.SpotifyError.Network(IllegalStateException("hors ligne"))
        player.emitNowPlaying(nowPlaying())
        assertNull(subject.isSaved.value)

        subject.toggleSave()

        assertEquals(false, subject.isSaved.value)
    }

    @Test
    fun `previousTrack passe au titre precedent des le debut du titre`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)
        player.emitNowPlaying(nowPlaying().copy(positionMs = 1_000))

        subject.previousTrack()

        assertEquals(listOf<PlayerCommand>(PlayerCommand.SkipPrevious), player.commands)
    }

    @Test
    fun `previousTrack passe 3 secondes enchaine deux appels, le premier ne faisant que redemarrer`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)
        player.emitNowPlaying(nowPlaying().copy(positionMs = 45_000))

        subject.previousTrack()

        assertEquals(listOf<PlayerCommand>(PlayerCommand.SkipPrevious, PlayerCommand.SkipPrevious), player.commands)
    }

    @Test
    fun `previousTrack sans lecture ne fait rien`() {
        val subject = viewModel()
        client.emitSession(SessionState.Authorized)

        subject.previousTrack()

        assertEquals(emptyList<PlayerCommand>(), player.commands)
    }
}
