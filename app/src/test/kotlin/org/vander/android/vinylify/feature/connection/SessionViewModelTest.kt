package org.vander.android.vinylify.feature.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.auth.SessionState
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class SessionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `au demarrage la session stockee est reprise puis le splash se leve`() {
        val client = FakeSpotifyClient(initialSession = SessionState.Authorized)

        val subject = SessionViewModel(client)

        assertEquals(listOf<SpotifyCall>(SpotifyCall.RestoreSession), client.calls)
        assertFalse(subject.restoring.value)
        assertEquals(SessionState.Authorized, subject.session.value)
    }

    @Test
    fun `sans jeton le splash se leve quand meme et la session reste deconnectee`() {
        val client = FakeSpotifyClient()

        val subject = SessionViewModel(client)

        assertFalse(subject.restoring.value)
        assertEquals(SessionState.SignedOut, subject.session.value)
    }

    @Test
    fun `signIn demande l'autorisation et la session devient Authorized`() {
        val client = FakeSpotifyClient()
        val subject = SessionViewModel(client)

        subject.signIn()

        assertEquals(SessionState.Authorized, subject.session.value)
        assertEquals(SpotifyCall.SignIn, client.calls.last())
    }
}
