package org.vander.android.vinylify.feature.library

import org.vander.android.vinylify.feature.ContentKind
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.feature.home.HomeFilter
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.Album
import org.vander.spotify.model.AlbumCollection
import org.vander.spotify.model.Artist
import org.vander.spotify.model.ArtistCollection
import org.vander.spotify.model.Playlist
import org.vander.spotify.model.PlaylistCollection
import org.vander.spotify.model.PlaylistId
import org.vander.spotify.testing.FakeSpotifyClient

class LibraryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient().apply { savedTracksCount = 1284 }

    private val nuits = Album(id = "a1", name = "Nuits blanches", artists = listOf(Artist("r1", "Elia Faure")))
    private val soir = Playlist(id = PlaylistId("p1"), name = "Le soir tard", trackCount = 38)
    private val cleo = Artist(id = "r2", name = "Cléo Wend")

    private fun loadedViewModel(): LibraryViewModel {
        client.emitSavedAlbums(AlbumCollection(listOf(nuits)))
        client.emitPlaylists(PlaylistCollection(listOf(soir)))
        client.emitFollowedArtists(ArtistCollection(listOf(cleo)))
        return LibraryViewModel(client)
    }

    @Test
    fun `Tout liste albums, playlists puis artistes avec leur genre`() =
        runTest {
            loadedViewModel().state.test {
                val rows = expectMostRecentItem().rows
                assertEquals(listOf(ContentKind.Album, ContentKind.Playlist, ContentKind.Artist), rows.map { it.kind })
                assertEquals("Elia Faure", rows.first().detail)
                assertEquals(38, rows[1].trackCount)
            }
        }

    @Test
    fun `chaque filtre ne garde que son genre`() =
        runTest {
            val subject = loadedViewModel()

            subject.state.test {
                expectMostRecentItem()
                HomeFilter.entries.drop(1).zip(ContentKind.entries).forEach { (filter, kind) ->
                    subject.onFilterSelect(filter)
                    assertEquals(listOf(kind), expectMostRecentItem().rows.map { it.kind })
                }
            }
        }

    @Test
    fun `le total de titres likes n'est lu qu'une fois la session autorisee`() =
        runTest {
            val subject = loadedViewModel()

            subject.state.test {
                assertEquals(null, expectMostRecentItem().likedCount)

                client.emitSession(SessionState.Authorized)

                assertEquals(1284, expectMostRecentItem().likedCount)
            }
        }
}
