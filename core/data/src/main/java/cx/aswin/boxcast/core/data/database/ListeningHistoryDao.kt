package cx.aswin.boxcast.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: ListeningHistoryEntity)
    
    // Fetch Top 7 items for the new Split UI (1st is Hero, 2-7 are Grid)
    @Query("SELECT * FROM listening_history WHERE isCompleted = 0 ORDER BY lastPlayedAt DESC LIMIT 7")
    fun getResumeItems(): Flow<List<ListeningHistoryEntity>>
    
    @Query("SELECT * FROM listening_history ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<ListeningHistoryEntity>>
    
    @Query("SELECT * FROM listening_history WHERE isDirty = 1")
    suspend fun getDirtyItems(): List<ListeningHistoryEntity>
    
    @Query("UPDATE listening_history SET isDirty = 0, syncedAt = :timestamp WHERE episodeId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, timestamp: Long)

    @Query("DELETE FROM listening_history WHERE episodeId = :episodeId")
    suspend fun delete(episodeId: String)

    @Query("SELECT * FROM listening_history WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getHistoryItem(episodeId: String): ListeningHistoryEntity?
}
