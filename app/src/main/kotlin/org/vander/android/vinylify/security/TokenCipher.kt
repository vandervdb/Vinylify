package org.vander.android.vinylify.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.GeneralSecurityException

/**
 * Chiffrement d'un jeton. Une interface étroite entre [EncryptedTokenStore] et Tink : le store se
 * teste sur la JVM avec un faux chiffre, sans Keystore ni appareil.
 *
 * Les deux méthodes lèvent [GeneralSecurityException] quand l'opération est impossible
 * (déchiffrement d'un contenu altéré ou chiffré avec une autre clé).
 */
interface TokenCipher {
    @Throws(GeneralSecurityException::class)
    fun encrypt(plain: ByteArray): ByteArray

    @Throws(GeneralSecurityException::class)
    fun decrypt(encrypted: ByteArray): ByteArray
}

/**
 * AES-256-GCM par Tink, avec une clé maître dans l'Android Keystore.
 *
 * Deux niveaux de clé, c'est le schéma que Tink recommande : un *keyset* AES-GCM chiffre les
 * données, et ce keyset est lui-même conservé chiffré dans des SharedPreferences par une clé
 * maître qui, elle, ne sort jamais du Keystore (matériel sécurisé quand l'appareil en a un).
 * Copier les fichiers de l'application ne suffit donc pas à lire le jeton.
 *
 * [ASSOCIATED_DATA] lie le chiffré à son usage : un contenu chiffré pour autre chose avec la même
 * clé ne se déchiffre pas ici.
 */
class TinkTokenCipher(
    context: Context,
) : TokenCipher {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager
            .Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun encrypt(plain: ByteArray): ByteArray = aead.encrypt(plain, ASSOCIATED_DATA)

    override fun decrypt(encrypted: ByteArray): ByteArray = aead.decrypt(encrypted, ASSOCIATED_DATA)

    companion object {
        internal const val KEYSET_PREFS = "vinylotech_keyset_prefs"
        private const val KEYSET_NAME = "spotify_token_keyset"
        private const val MASTER_KEY_URI = "android-keystore://vinylotech_token_master_key"
        private val ASSOCIATED_DATA = "vinylotech.spotify.token.v1".encodeToByteArray()
    }
}
