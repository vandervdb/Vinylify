package org.vander.android.vinylify.feature.search

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.SpotifyError
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.model.Album
import org.vander.spotify.model.SearchResults
import org.vander.spotify.model.Track
import org.vander.spotify.model.TrackId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class SearchViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()
    private val subject = SearchViewModel(client, player)

    private val nuits = Album(id = "a1", name = "Nuits blanches")

    @Test
    fun `une rafale de frappes ne declenche qu'une recherche, apres le debounce`() =
        runTest {
            client.searchResults = SearchResults(albums = listOf(nuits))

            "elia".forEachIndexed { i, _ -> subject.onQueryChange("elia".take(i + 1)) }
            advanceTimeBy(SearchViewModel.DEBOUNCE_MS - 1)
            assertEquals(emptyList<SpotifyCall>(), client.calls)

            advanceUntilIdle()

            assertEquals(listOf<SpotifyCall>(SpotifyCall.Search("elia")), client.calls)
            assertEquals(SearchStatus.Done, subject.state.value.status)
            assertEquals(listOf("a1"), subject.state.value.results.albums.map { it.id })
        }

    @Test
    fun `choisir un genre relance la recherche sans debounce et ne garde que ce genre`() =
        runTest {
            client.searchResults = SearchResults(albums = listOf(nuits), tracks = listOf(Track(TrackId("t1"), "Sous la neige")))
            subject.onQueryChange("elia")
            advanceUntilIdle()

            subject.onFilterSelect(SearchFilter.Tracks)
            // Aucun avancement du temps virtuel : la relance est immédiate.
            runCurrent()

            assertEquals(2, client.calls.size)
            assertEquals(SearchFilter.Tracks, subject.state.value.filter)
            assertEquals(emptyList<Album>(), subject.state.value.results.albums)
            assertEquals(listOf("t1"), subject.state.value.results.tracks.map { it.id.value })
        }

    @Test
    fun `le genre choisi survit a une nouvelle requete, et rechoisir le meme ne relance rien`() =
        runTest {
            client.searchResults = SearchResults(albums = listOf(nuits))
            subject.onFilterSelect(SearchFilter.Albums)
            assertEquals(emptyList<SpotifyCall>(), client.calls)

            subject.onQueryChange("elia")
            advanceUntilIdle()
            subject.onFilterSelect(SearchFilter.Albums)
            advanceUntilIdle()

            assertEquals(SearchFilter.Albums, subject.state.value.filter)
            assertEquals(1, client.calls.size)
        }

    @Test
    fun `vider le champ efface les resultats sans appel reseau`() =
        runTest {
            client.searchResults = SearchResults(albums = listOf(nuits))
            subject.onQueryChange("elia")
            advanceUntilIdle()

            subject.onQueryChange("")
            advanceUntilIdle()

            assertEquals(SearchStatus.Idle, subject.state.value.status)
            assertEquals(SearchResults.EMPTY, subject.state.value.results)
            assertEquals(1, client.calls.size)
        }

    @Test
    fun `un echec reseau passe en Failed et un nouvel essai reussit`() =
        runTest {
            client.searchResults = SearchResults(albums = listOf(nuits))
            client.nextFailure = SpotifyError.Api(500, "boom")

            subject.onQueryChange("elia")
            advanceUntilIdle()
            assertEquals(SearchStatus.Failed, subject.state.value.status)

            subject.onQueryChange("elia ")
            advanceUntilIdle()
            assertEquals(SearchStatus.Done, subject.state.value.status)
        }

    @Test
    fun `toucher un titre le lance`() =
        runTest {
            player.connect()

            subject.onTrackClick(Track(TrackId("t1"), "Sous la neige"))

            assertEquals(listOf<PlayerCommand>(PlayerCommand.Play(TrackId("t1"))), player.commands)
        }
}
