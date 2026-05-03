package cx.aswin.boxcast.core.data.database.radio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedStationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: FollowedStationEntity)

    @Query("DELETE FROM followed_stations WHERE id = :stationId")
    suspend fun delete(stationId: String)

    @Query("SELECT * FROM followed_stations ORDER BY followedAt DESC")
    fun getAllFollowed(): Flow<List<FollowedStationEntity>>

    @Query("SELECT id FROM followed_stations")
    fun getFollowedIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_stations WHERE id = :stationId)")
    suspend fun isFollowed(stationId: String): Boolean
}
