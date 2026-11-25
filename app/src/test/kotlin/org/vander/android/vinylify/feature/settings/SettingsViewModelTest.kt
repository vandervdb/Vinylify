package org.vander.android.vinylify.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.vander.android.vinylify.testing.MainDispatcherRule
import org.vander.spotify.android.testing.FakeSpotifyPlayer
import org.vander.spotify.android.testing.PlayerCommand
import org.vander.spotify.auth.SessionState
import org.vander.spotify.model.User
import org.vander.spotify.model.UserId
import org.vander.spotify.testing.FakeSpotifyClient
import org.vander.spotify.testing.SpotifyCall

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val client = FakeSpotifyClient().apply { user = User(UserId("u1"), displayName = "Théo") }
    private val player = FakeSpotifyPlayer()

    @Test
    fun `le nom n'est charge qu'une fois la session autorisee`() {
        val subject = SettingsViewModel(client, player)
        assertNull(subject.state.value.userName)

        client.emitSession(SessionState.Authorized)

        assertEquals("Théo", subject.state.value.userName)
    }

    @Test
    fun `sans nom affiche, l'identifiant sert de repli`() {
        client.user = User(UserId("raoul"), displayName = null)
        val subject = SettingsViewModel(client, player)

        client.emitSession(SessionState.Authorized)

        assertEquals("raoul", subject.state.value.userName)
    }

    @Test
    fun `signOut deconnecte le client`() {
        client.emitSession(SessionState.Authorized)
        val subject = SettingsViewModel(client, player)

        subject.signOut()

        assertEquals(SessionState.SignedOut, client.session.value)
        assertEquals(SpotifyCall.SignOut, client.calls.last())
    }

    @Test
    fun `signOut met la lecture en pause`() {
        runBlocking { player.connect() }
        client.emitSession(SessionState.Authorized)
        val subject = SettingsViewModel(client, player)

        subject.signOut()

        assertTrue(PlayerCommand.Pause in player.commands)
        assertEquals(SessionState.SignedOut, client.session.value)
    }

    @Test
    fun `signOut deconnecte meme quand la liaison est fermee`() {
        // Sans liaison, la pause échoue : elle ne doit pas empêcher la déconnexion.
        client.emitSession(SessionState.Authorized)
        val subject = SettingsViewModel(client, player)

        subject.signOut()

        assertTrue(player.commands.isEmpty())
        assertEquals(SessionState.SignedOut, client.session.value)
    }
}
