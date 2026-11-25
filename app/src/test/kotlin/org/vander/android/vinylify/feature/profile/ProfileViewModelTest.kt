package org.vander.android.vinylify.feature.profile

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.AlbumCollection
import org.vander.spotify.model.ArtistCollection
import org.vander.spotify.model.User
import org.vander.spotify.model.UserId
import org.vander.spotify.testing.FakeSpotifyClient

class ProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client =
        FakeSpotifyClient().apply {
            user = User(UserId("u1"), displayName = "Théo", product = "premium")
            savedTracksCount = 1284
        }

    @Test
    fun `le profil se remplit une fois la session autorisee`() =
        runTest {
            client.emitSavedAlbums(AlbumCollection(total = 243))
            client.emitFollowedArtists(ArtistCollection(total = 61))
            val subject = ProfileViewModel(client)

            subject.state.test {
                assertEquals(null, expectMostRecentItem().name)

                client.emitSession(SessionState.Authorized)

                val state = expectMostRecentItem()
                assertEquals("Théo", state.name)
                assertEquals(true, state.isPremium)
                assertEquals(243, state.albumCount)
                assertEquals(1284, state.likedCount)
                assertEquals(61, state.followingCount)
            }
        }
}
