package org.vander.android.vinylify.data.history

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Une écoute telle que l'historique Spotify la rend, à plat : Spotify n'en garde qu'une
 * cinquantaine, l'app les accumule pour remonter plus loin.
 *
 * La clé (instant, titre) rend l'insertion idempotente : chaque chargement renvoie en grande partie
 * les mêmes écoutes.
 *
 * @property artists et [albumArtists] : `id` et nom alternés, séparés par [FIELD_SEPARATOR].
 */
@Entity(tableName = "play_history", primaryKeys = ["playedAtMs", "trackId"])
data class PlayHistoryEntity(
    val playedAtMs: Long,
    val trackId: String,
    val trackName: String,
    val durationMs: Long,
    val artists: String,
    val albumId: String?,
    val albumName: String?,
    val albumArtists: String?,
    val albumCoverUrl: String?,
    val albumTrackCount: Int,
    val contextType: String?,
    val contextId: String?,
)

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAtMs DESC")
    fun observeAll(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(plays: List<PlayHistoryEntity>)

    @Query("DELETE FROM play_history WHERE playedAtMs < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}

@Database(entities = [PlayHistoryEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun playHistory(): PlayHistoryDao
}

internal const val FIELD_SEPARATOR = '\u001F'
