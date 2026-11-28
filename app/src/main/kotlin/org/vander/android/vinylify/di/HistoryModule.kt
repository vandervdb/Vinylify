package org.vander.android.vinylify.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vander.android.vinylify.data.history.HistoryDatabase
import org.vander.android.vinylify.data.history.PlayHistoryDao
import org.vander.android.vinylify.data.history.PlayHistoryStore
import org.vander.android.vinylify.data.history.RoomPlayHistoryStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds
    abstract fun bindPlayHistoryStore(store: RoomPlayHistoryStore): PlayHistoryStore

    companion object {
        @Provides
        @Singleton
        fun provideHistoryDatabase(
            @ApplicationContext context: Context,
        ): HistoryDatabase = Room.databaseBuilder(context, HistoryDatabase::class.java, "history.db").build()

        @Provides
        fun providePlayHistoryDao(database: HistoryDatabase): PlayHistoryDao = database.playHistory()
    }
}
