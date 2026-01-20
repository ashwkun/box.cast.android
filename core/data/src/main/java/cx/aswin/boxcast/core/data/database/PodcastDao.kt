package cx.aswin.boxcast.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(podcast: PodcastEntity)
    
    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title ASC")
    fun getSubscribedPodcasts(): Flow<List<PodcastEntity>>
    
    @Query("SELECT * FROM podcasts WHERE podcastId = :id")
    suspend fun getPodcast(id: String): PodcastEntity?
    
    @Query("UPDATE podcasts SET isSubscribed = :isSubscribed WHERE podcastId = :id")
    suspend fun setSubscribed(id: String, isSubscribed: Boolean)
}
