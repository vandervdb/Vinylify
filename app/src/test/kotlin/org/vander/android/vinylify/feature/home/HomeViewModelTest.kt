package org.vander.android.vinylify.feature.home

import org.vander.android.vinylify.feature.ContentKind
import app.cash.turbine.test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.SpotifyError
import org.vander.spotify.auth.SessionState
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.model.Album
import org.vander.spotify.model.AlbumCollection
import org.vander.spotify.model.Artist
import org.vander.spotify.model.ArtistCollection
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistCollection
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient()
    private val player = FakeSpotifyPlayer()

    private val nuits =
        Album(id = "a1", name = "Nuits blanches", artists = listOf(Artist("r1", "Elia Faure")), releaseYear = 2023, trackCount = 11)
    private val maree = Album(id = "a2", name = "Marée haute")
    private val sillons = Playlist(id = PlaylistId("p1"), name = "Sillons")
    private val cleo = Artist(id = "r1", name = "Cléo Wend")

    private fun viewModel() = HomeViewModel(client, player)

    @Test
    fun `rien n'est charge tant que la session n'est pas autorisee`() =
        runTest {
            viewModel()

            assertTrue(client.calls.none { it is SpotifyCall.RefreshSavedAlbums })
        }

    @Test
    fun `une fois autorisee, albums et playlists sont rafraichis`() =
        runTest {
            viewModel()

            client.emitSession(SessionState.Authorized)

            assertTrue(SpotifyCall.RefreshSavedAlbums in client.calls)
            assertTrue(SpotifyCall.RefreshPlaylists in client.calls)
            assertTrue(SpotifyCall.RefreshFollowedArtists in client.calls)
        }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun `un chargement qui echoue est retente`() =
        runTest {
            val subject = viewModel()
            client.nextFailure = SpotifyError.Network(RuntimeException("jeton pas prêt"))

            client.emitSession(SessionState.Authorized)
            advanceUntilIdle()

            assertEquals(2, client.calls.count { it is SpotifyCall.RefreshSavedAlbums })
            subject.state.test {
                assertEquals(false, awaitItem().loadFailed)
            }
        }

    @Test
    fun `le heros resume le premier album avec artiste, annee et nombre de titres`() =
        runTest {
            client.emitSavedAlbums(AlbumCollection(listOf(nuits, maree), total = 243))
            val subject = viewModel()

            subject.state.test {
                val state = expectMostRecentItem()
                assertEquals("Nuits blanches", state.resume?.title)
                assertEquals("Elia Faure · 2023 · 11 titres", state.resume?.subtitle)
                assertEquals(243, state.libraryCount)
            }
        }

    @Test
    fun `le filtre Playlists n'affiche que les playlists`() =
        runTest {
            client.emitSavedAlbums(AlbumCollection(listOf(nuits)))
            client.emitPlaylists(PlaylistCollection(listOf(sillons)))
            val subject = viewModel()

            subject.state.test {
                assertEquals(listOf("a1", "p1"), expectMostRecentItem().tiles.map { it.id })

                subject.onFilterSelect(HomeFilter.Playlists)

                assertEquals(listOf("p1"), expectMostRecentItem().tiles.map { it.id })
            }
        }

    @Test
    fun `chaque onglet compte ce qu'il liste, Tout additionne`() =
        runTest {
            client.emitSavedAlbums(AlbumCollection(listOf(nuits), total = 40))
            client.emitPlaylists(PlaylistCollection(listOf(sillons), total = 12))
            client.emitFollowedArtists(ArtistCollection(listOf(cleo), total = 61))
            val subject = viewModel()

            subject.state.test {
                assertEquals(113, expectMostRecentItem().libraryCount)

                subject.onFilterSelect(HomeFilter.Albums)
                assertEquals(40, expectMostRecentItem().libraryCount)

                subject.onFilterSelect(HomeFilter.Playlists)
                assertEquals(12, expectMostRecentItem().libraryCount)

                subject.onFilterSelect(HomeFilter.Artists)
                val artists = expectMostRecentItem()
                assertEquals(61, artists.libraryCount)
                assertEquals(listOf("r1"), artists.tiles.map { it.id })
            }
        }

    @Test
    fun `le heros n'apparait que sur Tout et Albums`() =
        runTest {
            client.emitSavedAlbums(AlbumCollection(listOf(nuits)))
            val subject = viewModel()

            subject.state.test {
                assertEquals("Nuits blanches", expectMostRecentItem().resume?.title)

                subject.onFilterSelect(HomeFilter.Playlists)
                assertEquals(null, expectMostRecentItem().resume)

                subject.onFilterSelect(HomeFilter.Artists)
                assertEquals(null, expectMostRecentItem().resume)

                subject.onFilterSelect(HomeFilter.Albums)
                assertEquals("Nuits blanches", expectMostRecentItem().resume?.title)
            }
        }
}
