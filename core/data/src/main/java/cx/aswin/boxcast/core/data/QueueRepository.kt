package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.data.database.BoxCastDatabase
import cx.aswin.boxcast.core.data.database.entities.QueueItem
import cx.aswin.boxcast.core.network.model.EpisodeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepository @Inject constructor(
    private val database: BoxCastDatabase,
    private val podcastRepository: PodcastRepository
) {
    private val queueDao = database.queueDao()

    val queue: Flow<List<EpisodeItem>> = queueDao.getAllQueueItems()
        .map { items ->
            // Map queue items to full Episode objects using embedded metadata
            items.map { it.toEpisodeItem() }
        }

    suspend fun addToQueue(episode: EpisodeItem, podcast: cx.aswin.boxcast.core.model.Podcast?) {
        val maxPos = queueDao.getMaxPosition() ?: 0
        
        // Ensure we have podcast metadata. If null, we might be in trouble, 
        // but EpisodeItem often has feedImage. 
        // We'll use defaults if missing.
        val podcastTitle = podcast?.title ?: "Unknown Podcast"
        val podcastId = podcast?.id?.toLongOrNull() ?: 0L 
        // Note: Podcast.id is String, QueueItem.podcastId is Long. 
        // We need to be careful with ID types. 
        // PodcastIndex IDs are Longs, but our model uses String.
        // Let's rely on standard parsing.

        val newItem = QueueItem(
            episodeId = episode.id,
            title = episode.title,
            podcastId = podcastId,
            podcastTitle = podcastTitle,
            imageUrl = episode.image ?: podcast?.imageUrl,
            audioUrl = episode.enclosureUrl ?: "",
            duration = episode.duration ?: 0,
            pubDate = episode.datePublished ?: 0L,
            description = episode.description,
            position = maxPos + 1
        )
        queueDao.insertQueueItem(newItem)
    }

    suspend fun clearQueue() {
        queueDao.clearQueue()
    }
    
    suspend fun getQueueSnapshot(): List<cx.aswin.boxcast.core.model.Episode> {
        val items = queueDao.getAllQueueItemsSync()
        return items.map { it.toDomainEpisode() }
    }
    
    private fun cx.aswin.boxcast.core.data.database.entities.QueueItem.toDomainEpisode(): cx.aswin.boxcast.core.model.Episode {
        return cx.aswin.boxcast.core.model.Episode(
            id = this.episodeId.toString(),
            title = this.title,
            description = this.description ?: "",
            audioUrl = this.audioUrl,
            imageUrl = this.imageUrl,
            podcastImageUrl = this.imageUrl, // Fallback
            podcastTitle = this.podcastTitle,
            duration = this.duration ?: 0,
            publishedDate = this.pubDate
        )
    }

    suspend fun reorderQueue(items: List<EpisodeItem>) {
        // This is tricky. We need to map EpisodeItems back to QueueItems to update positions.
        // But the UI only sees EpisodeItems.
        // We should probably expose QueueItem or a wrapper to the UI if reordering is needed,
        // OR finding the QueueItem by episodeId.
        // For now, let's defer reordering implementation or fetch IDs.
    }
}
