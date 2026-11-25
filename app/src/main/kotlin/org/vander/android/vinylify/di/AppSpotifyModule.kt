package org.vander.android.vinylify.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.vander.android.vinylify.BuildConfig
import org.vander.spotify.LyricsRepository
import org.vander.spotify.Spotify
import org.vander.spotify.SpotifyConfig
import org.vander.spotify.android.auth.CustomTabAuthorizer
import org.vander.spotify.auth.Authorizer

/**
 * Les deux liaisons que `spotify-android-hilt` laisse à l'application : le `clientId` et la
 * redirect URI lui appartiennent, la lib ne les connaît pas.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppSpotifyModule {
    @Provides
    fun provideSpotifyConfig(): SpotifyConfig =
        SpotifyConfig(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
        )

    /** Singleton : l'instance porte le cache des titres déjà cherchés. */
    @Provides
    @Singleton
    fun provideLyricsRepository(): LyricsRepository = Spotify.lyricsRepository()

    @Provides
    fun provideAuthorizer(
        @ApplicationContext context: Context,
    ): Authorizer = CustomTabAuthorizer(context)
}
