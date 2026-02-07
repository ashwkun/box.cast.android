package cx.aswin.boxcast.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey
    val podcastId: String,
    
    val title: String,
    val author: String,
    val imageUrl: String,
    val description: String?,
    
    // Subscription State
    val isSubscribed: Boolean = false,
    
    // Genre for Smart Queue matching
    val genre: String? = null,
    
    val lastRefreshed: Long = 0
)
