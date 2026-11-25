package org.vander.android.vinylify.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.vander.spotify.auth.StoredToken
import java.io.File
import java.security.GeneralSecurityException

class EncryptedTokenStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Chiffre réversible et lisible dans un test : XOR avec une clé, et un sceau pour détecter une altération. */
    private class FakeCipher(
        private val key: Byte = 0x5A,
    ) : TokenCipher {
        override fun encrypt(plain: ByteArray): ByteArray = byteArrayOf(SEAL) + plain.map { (it.toInt() xor key.toInt()).toByte() }

        override fun decrypt(encrypted: ByteArray): ByteArray {
            if (encrypted.firstOrNull() != SEAL) throw GeneralSecurityException("contenu altéré")
            return encrypted.drop(1).map { (it.toInt() xor key.toInt()).toByte() }.toByteArray()
        }

        companion object {
            const val SEAL: Byte = 0x42
        }
    }

    private val token = StoredToken(accessToken = "ACCESS-secret-123", refreshToken = "REFRESH-secret-456", expiresAtEpochMs = 1_700_000_000_000)

    private fun file() = File(tmp.root, "token.preferences_pb")

    private fun TestScope.store(cipher: TokenCipher = FakeCipher()) =
        EncryptedTokenStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { file() }, cipher)

    @Test
    fun `un jeton sauvegarde se relit a l'identique`() =
        runTest {
            val subject = store()

            subject.save(token)

            assertEquals(token, subject.load())
        }

    @Test
    fun `rien n'est lisible en clair sur le disque`() =
        runTest {
            store().save(token)

            val onDisk = file().readBytes().decodeToString(throwOnInvalidSequence = false)

            assertFalse("l'access token ne doit pas apparaitre", "ACCESS-secret-123" in onDisk)
            assertFalse("le refresh token ne doit pas apparaitre", "REFRESH-secret-456" in onDisk)
        }

    @Test
    fun `un refresh token absent reste absent`() =
        runTest {
            val subject = store()

            subject.save(token.copy(refreshToken = null))

            assertNull(subject.load()?.refreshToken)
        }

    @Test
    fun `sans jeton load rend null`() =
        runTest {
            assertNull(store().load())
        }

    @Test
    fun `clear efface le jeton`() =
        runTest {
            val subject = store()
            subject.save(token)

            subject.clear()

            assertNull(subject.load())
        }

    @Test
    fun `un jeton chiffre avec une autre cle est un jeton absent et il est efface`() =
        runTest {
            val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file() }
            EncryptedTokenStore(dataStore, FakeCipher(key = 0x11)).save(token)
            // Autre appareil, clé du Keystore invalidée : le chiffré ne se déchiffre plus.
            val subject = EncryptedTokenStore(dataStore, AlwaysFailing)

            assertNull(subject.load())
            assertNull("le contenu illisible est effacé", dataStore.data.first()[stringPreferencesKey("token")])
        }

    @Test
    fun `un contenu corrompu est un jeton absent`() =
        runTest {
            val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file() }
            dataStore.edit { it[stringPreferencesKey("token")] = "%%%pas-du-base64%%%" }

            assertNull(EncryptedTokenStore(dataStore, FakeCipher()).load())
        }

    private object AlwaysFailing : TokenCipher {
        override fun encrypt(plain: ByteArray): ByteArray = plain

        override fun decrypt(encrypted: ByteArray): ByteArray = throw GeneralSecurityException("clé invalide")
    }
}
