package org.vander.android.vinylify.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vander.android.vinylify.security.EncryptedTokenStore
import org.vander.spotify.auth.TokenStore
import javax.inject.Singleton

/**
 * Fournit le [TokenStore] à `spotify-android-hilt`, qui le prend comme liaison facultative : sans
 * ce module, la lib conserverait le jeton en clair.
 *
 * `@Singleton` est structurel : DataStore refuse deux instances actives sur le même fichier.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: Context,
    ): TokenStore = EncryptedTokenStore.create(context)
}
