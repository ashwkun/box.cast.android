package cx.aswin.boxcast.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cx.aswin.boxcast.core.network.model.EpisodeItem

@Entity(tableName = "queue_items")
data class QueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Episode Metadata (Embedded since we don't have a central Episode table)
    val episodeId: Long, // Or String depending on ID type? API returns Long usually.
    val title: String,
    val podcastId: Long, // Or String?
    val podcastTitle: String,
    val podcastGenre: String = "", // NEW: For smart fallback matching
    val podcastImageUrl: String? = null, // NEW: For podcast artwork fallback
    val imageUrl: String?,
    val audioUrl: String,
    val duration: Int,
    val pubDate: Long,
    val description: String?,
    
    // Queue Metadata
    val position: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val contextType: String = "MANUAL", // MANUAL, NEXT_UP, SMART_RECOMMENDATION
    val contextSourceId: String? = null // Podcast ID, Vibe ID, etc.
) {
    fun toEpisodeItem(): EpisodeItem {
        return EpisodeItem(
            id = episodeId,
            title = title,
            enclosureUrl = audioUrl,
            description = description ?: "",
            duration = duration,
            datePublished = pubDate, // Ensure type matches (Long vs Int)
            image = imageUrl ?: ""
        )
    }
}
