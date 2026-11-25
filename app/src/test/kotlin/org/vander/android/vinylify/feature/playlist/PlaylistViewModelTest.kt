package org.vander.android.vinylify.feature.playlist

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.feature.DetailState
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistDetail
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class PlaylistViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()
    private val playlist = Playlist(PlaylistId("p1"), "Le soir tard")
    private val braise = Track(TrackId("t1"), "Braise")

    private fun viewModel(id: String = "p1") =
        PlaylistViewModel(SavedStateHandle(mapOf(PlaylistRoute.ARG_ID to id)), client, player)

    @Test
    fun `la playlist de la route est chargee au demarrage`() {
        client.playlistDetails["p1"] = PlaylistDetail(playlist, tracks = listOf(braise))

        val subject = viewModel()

        assertEquals(listOf<SpotifyCall>(SpotifyCall.GetPlaylist(PlaylistId("p1"))), client.calls)
        assertEquals(DetailState.Loaded(PlaylistDetail(playlist, tracks = listOf(braise))), subject.state.value)
    }

    @Test
    fun `une playlist introuvable passe en Failed`() {
        assertEquals(DetailState.Failed, viewModel("inconnue").state.value)
    }

    @Test
    fun `playPlaylist lance la playlist, playTrack lance le titre`() {
        client.playlistDetails["p1"] = PlaylistDetail(playlist, tracks = listOf(braise))
        val subject = viewModel()
        runBlocking { player.connect() }

        subject.playPlaylist()
        subject.playTrack(braise)

        assertEquals(listOf(PlayerCommand.PlayPlaylist(playlist), PlayerCommand.Play(TrackId("t1"))), player.commands)
    }
}
