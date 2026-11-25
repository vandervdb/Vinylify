package org.vander.android.vinylify.feature.artist

import org.vander.spotify.testing.SpotifyCall
import org.vander.spotify.SpotifyError
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.feature.DetailState
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.model.Artist
import org.vander.spotify.model.ArtistDetail
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient

class ArtistViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()
    private val cleo = Artist(id = "r1", name = "Cléo Wend")

    private fun viewModel() = ArtistViewModel(SavedStateHandle(mapOf(ArtistRoute.ARG_ID to "r1")), client, player)

    @Test
    fun `l'artiste de la route est charge au demarrage`() {
        val detail = ArtistDetail(cleo, topTracks = listOf(Track(TrackId("t1"), "Braise")))
        client.artistDetails["r1"] = detail

        assertEquals(DetailState.Loaded(detail), viewModel().state.value)
    }

    @Test
    fun `un artiste introuvable passe en Failed`() {
        assertEquals(DetailState.Failed, viewModel().state.value)
    }

    @Test
    fun `playArtist lance l'artiste`() {
        client.artistDetails["r1"] = ArtistDetail(cleo)
        val subject = viewModel()
        runBlocking { player.connect() }

        subject.playArtist()

        assertEquals(listOf<PlayerCommand>(PlayerCommand.PlayArtist(cleo)), player.commands)
    }

    @Test
    fun `l'etat suivi est lu puis bascule et les artistes suivis sont relus`() {
        client.artistDetails["r1"] = ArtistDetail(cleo)
        val subject = viewModel()
        assertEquals(false, subject.isFollowing.value)

        subject.toggleFollow()

        assertEquals(true, subject.isFollowing.value)
        assertEquals(SpotifyCall.RefreshFollowedArtists, client.calls.last())

        subject.toggleFollow()
        assertEquals(false, subject.isFollowing.value)
    }

    @Test
    fun `un echec de suivi est signale et ne change pas l'etat`() {
        client.artistDetails["r1"] = ArtistDetail(cleo)
        val subject = viewModel()
        client.nextFailure = SpotifyError.Api(403, "Forbidden")

        subject.toggleFollow()

        assertEquals(false, subject.isFollowing.value)
        assertEquals(true, subject.followFailed.value)
    }
}
