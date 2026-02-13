package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.data.database.PodcastDao
import cx.aswin.boxcast.core.data.database.PodcastEntity
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(
    private val podcastDao: PodcastDao,
    private val analyticsHelper: cx.aswin.boxcast.core.data.analytics.AnalyticsHelper? = null // Optional for ease of migration/testing
) {

    val subscribedPodcastIds: Flow<Set<String>> = podcastDao.getSubscribedPodcasts()
        .map { list -> list.map { it.podcastId }.toSet() }

    fun getAllSubscribedPodcasts(): Flow<List<PodcastEntity>> {
        return podcastDao.getSubscribedPodcasts()
    }

    val subscribedPodcasts: Flow<List<Podcast>> = podcastDao.getSubscribedPodcasts()
        .map { list ->
            list.map { entity ->
                Podcast(
                    id = entity.podcastId,
                    title = entity.title,
                    artist = entity.author ?: "",
                    imageUrl = entity.imageUrl ?: "",
                    description = entity.description,
                    genre = entity.genre ?: "Podcast" // Use stored genre
                )
            }
        }

    suspend fun toggleSubscription(podcast: Podcast) {
        val existing = podcastDao.getPodcast(podcast.id)
        val isCurrentlySubscribed = existing?.isSubscribed == true

        if (isCurrentlySubscribed) {
            // Unsubscribe
            podcastDao.setSubscribed(podcast.id, false)
            analyticsHelper?.logUnsubscribe(podcast.title)
        } else {
            // Subscribe (Upsert to ensure we have data for offline/Jump Back In)
            val entity = PodcastEntity(
                podcastId = podcast.id,
                title = podcast.title,
                author = podcast.artist,
                imageUrl = podcast.imageUrl,
                description = podcast.description,
                isSubscribed = true,
                genre = podcast.genre, // Persist genre for Smart Queue matching
                lastRefreshed = System.currentTimeMillis()
            )
            podcastDao.upsert(entity)
            analyticsHelper?.logSubscribe(podcast.title)
        }
    }

    suspend fun isSubscribed(podcastId: String): Boolean {
        return podcastDao.getPodcast(podcastId)?.isSubscribed == true
    }

    suspend fun subscribe(podcast: Podcast) {
        val existing = podcastDao.getPodcast(podcast.id)
        if (existing == null) {
            val entity = PodcastEntity(
                podcastId = podcast.id,
                title = podcast.title,
                author = podcast.artist,
                imageUrl = podcast.imageUrl,
                description = podcast.description,
                isSubscribed = true,
                genre = podcast.genre,
                lastRefreshed = System.currentTimeMillis()
            )
            podcastDao.upsert(entity)
            analyticsHelper?.logSubscribe(podcast.title)
        } else if (!existing.isSubscribed) {
            podcastDao.setSubscribed(podcast.id, true)
            analyticsHelper?.logSubscribe(podcast.title)
        }
    }
}
