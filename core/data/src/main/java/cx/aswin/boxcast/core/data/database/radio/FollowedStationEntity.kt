package cx.aswin.boxcast.core.data.database.radio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_stations")
data class FollowedStationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val genre: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val country: String = "",
    val language: String = "",
    val followedAt: Long = System.currentTimeMillis()
)
