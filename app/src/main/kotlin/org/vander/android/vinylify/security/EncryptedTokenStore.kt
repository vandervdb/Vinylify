package org.vander.android.vinylify.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.vander.spotify.auth.StoredToken
import org.vander.spotify.auth.TokenStore
import java.io.File
import java.security.GeneralSecurityException
import java.util.Base64

/**
 * [TokenStore] qui ne conserve jamais le jeton en clair.
 *
 * L'access token et le refresh token sont sérialisés, chiffrés par [cipher] (AEAD), puis écrits
 * en Base64 dans un DataStore. Ce qui est sur le disque est donc inutilisable sans la clé, et la
 * clé ne quitte pas l'Android Keystore (voir [TinkTokenCipher]).
 *
 * **Un jeton illisible est un jeton absent.** Si le déchiffrement échoue — clé du Keystore
 * invalidée (nouvelle empreinte, changement de verrouillage), fichier restauré sur un autre
 * appareil, contenu corrompu — on efface et on rend `null` : l'utilisateur se reconnecte, ce qui
 * est sans gravité. Planter, ou pire redemander indéfiniment un jeton mort, le serait.
 */
class EncryptedTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : TokenStore {
    override suspend fun load(): StoredToken? {
        val encoded = dataStore.data.first()[TOKEN] ?: return null
        return try {
            val plain = cipher.decrypt(Base64.getDecoder().decode(encoded))
            Json.decodeFromString<Payload>(plain.decodeToString()).toStoredToken()
        } catch (_: GeneralSecurityException) {
            clear()
            null
        } catch (_: IllegalArgumentException) {
            // Base64 invalide.
            clear()
            null
        } catch (_: kotlinx.serialization.SerializationException) {
            clear()
            null
        }
    }

    override suspend fun save(token: StoredToken) {
        val plain = Json.encodeToString(Payload.from(token)).encodeToByteArray()
        val encoded = Base64.getEncoder().encodeToString(cipher.encrypt(plain))
        dataStore.edit { it[TOKEN] = encoded }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /** Forme écrite sur le disque, avant chiffrement. `StoredToken` vient de la lib, non sérialisable. */
    @Serializable
    private data class Payload(
        val accessToken: String,
        val refreshToken: String? = null,
        val expiresAtEpochMs: Long,
    ) {
        fun toStoredToken() = StoredToken(accessToken, refreshToken, expiresAtEpochMs)

        companion object {
            fun from(token: StoredToken) = Payload(token.accessToken, token.refreshToken, token.expiresAtEpochMs)
        }
    }

    companion object {
        private val TOKEN = stringPreferencesKey("token")

        private const val FILE_NAME = "spotify_token_encrypted.preferences_pb"

        /** Ancien fichier du `DataStoreTokenStore` de la lib : jeton en clair, à ne pas laisser traîner. */
        private const val LEGACY_FILE_NAME = "spotify_session.preferences_pb"

        /**
         * À n'appeler qu'une fois par processus : DataStore refuse plusieurs instances actives sur un
         * même fichier. Supprime au passage l'ancien fichier en clair, s'il existe.
         */
        fun create(context: Context): EncryptedTokenStore {
            val dir = File(context.applicationContext.filesDir, "datastore")
            File(dir, LEGACY_FILE_NAME).delete()
            return EncryptedTokenStore(
                dataStore = PreferenceDataStoreFactory.create { File(dir, FILE_NAME) },
                cipher = TinkTokenCipher(context.applicationContext),
            )
        }
    }
}
