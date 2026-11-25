package org.vander.android.vinylify.feature.album

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.feature.DetailState
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.model.Album
import org.vander.spotify.model.AlbumDetail
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class AlbumViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()
    private val album = Album(id = "a1", name = "Marée haute")
    private val estran = Track(TrackId("t1"), "Estran", album = album, trackNumber = 1)

    private fun viewModel(id: String = "a1") = AlbumViewModel(SavedStateHandle(mapOf(AlbumRoute.ARG_ID to id)), client, player)

    @Test
    fun `l'album de la route est charge au demarrage`() {
        client.albumDetails["a1"] = AlbumDetail(album, listOf(estran))

        val subject = viewModel()

        assertEquals(listOf<SpotifyCall>(SpotifyCall.GetAlbum("a1")), client.calls)
        assertEquals(DetailState.Loaded(AlbumDetail(album, listOf(estran))), subject.state.value)
    }

    @Test
    fun `un album introuvable passe en Failed et load permet de reessayer`() {
        val subject = viewModel("inconnu")
        assertEquals(DetailState.Failed, subject.state.value)

        client.albumDetails["inconnu"] = AlbumDetail(album)
        subject.load()

        assertTrue(subject.state.value is DetailState.Loaded)
    }

    @Test
    fun `playAlbum lance l'album, playTrack lance le titre`() {
        client.albumDetails["a1"] = AlbumDetail(album, listOf(estran))
        val subject = viewModel()
        player.connectBlocking()

        subject.playAlbum()
        subject.playTrack(estran)

        assertEquals(listOf(PlayerCommand.PlayAlbum(album), PlayerCommand.Play(TrackId("t1"))), player.commands)
    }

    @Test
    fun `playAlbum ne fait rien tant que l'album n'est pas charge`() {
        val subject = viewModel("inconnu")
        player.connectBlocking()

        subject.playAlbum()

        assertEquals(emptyList<PlayerCommand>(), player.commands)
    }

    private fun FakeSpotifyPlayer.connectBlocking() = kotlinx.coroutines.runBlocking { connect() }
}
