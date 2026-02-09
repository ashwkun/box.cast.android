package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.data.database.ListeningHistoryDao
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.network.model.EpisodeItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Represents an episode in the queue along with its podcast context.
 * This is needed because fallback episodes may come from different podcasts.
 */
data class QueueEntry(
    val episode: EpisodeItem,
    val podcast: Podcast
)

interface SmartQueueEngine {
    suspend fun getNextEpisodes(currentEpisode: EpisodeItem, podcast: Podcast?, preferredSort: String? = null): List<QueueEntry>
}

@Singleton
class DefaultSmartQueueEngine @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val listeningHistoryDao: ListeningHistoryDao,
    private val subscriptionRepository: SubscriptionRepository
) : SmartQueueEngine {

    override suspend fun getNextEpisodes(currentEpisode: EpisodeItem, podcast: Podcast?, preferredSort: String?): List<QueueEntry> {
        android.util.Log.d("SmartQueue", "getNextEpisodes called for epId=${currentEpisode.id}, podcast=${podcast?.title}, sort=$preferredSort")
        
        if (podcast == null) {
            android.util.Log.e("SmartQueue", "Podcast is null, returning empty")
            return emptyList()
        }

        // 1. Fetch all episodes for context (Network call)
        // We use the new paginated endpoint in repo to get up to 1000 items
        val rawEpisodes = podcastRepository.getEpisodes(podcast.id)
        android.util.Log.d("SmartQueue", "Fetched ${rawEpisodes.size} episodes from repo for context")
        
        if (rawEpisodes.isEmpty()) {
             android.util.Log.w("SmartQueue", "Repo returned NO episodes")
             return emptyList()
        }

        // 1.5 ALWAYS Sort Chronologically (Oldest -> Newest)
        // User Request: "no one wants to listen to pods in a reverse cron order"
        // Even if they sort UI by Newest, if they play Ep 50, they likely want Ep 51 next, not Ep 49.
        
        android.util.Log.d("SmartQueue", "Sorting strategy: ALWAYS Chronological (Oldest -> Newest)")
        val allEpisodes = rawEpisodes.sortedBy { it.publishedDate }

        // 2. Find current episode index
        // Note: EpisodeItem.id is Long, PodcastRepository returns domain Episode with String id.
        // We need to compare carefully.
        val searchId = currentEpisode.id.toString()
        val currentIndex = allEpisodes.indexOfFirst { it.id == searchId }
        
        android.util.Log.d("SmartQueue", "Searching for ID $searchId in list. Found at index: $currentIndex. Top item: ${allEpisodes.firstOrNull()?.title}")

        if (currentIndex == -1) {
            android.util.Log.e("SmartQueue", "Current episode NOT found in context list of size ${allEpisodes.size}. Dumping first 5 IDs: ${allEpisodes.take(5).map { it.id }}")
            return emptyList()
        }

        // 3. Standard "Play Down The List" behavior
        // Whether it's Serial or Episodic, users usually expect the player to continue 
        // with the items visible below the current one in the list.
        // Assuming the list itself is sorted correctly (Newest->Oldest or Oldest->Newest),
        // we just take the next N items.
        
        val candidates = mutableListOf<Episode>()
        
        // Take next 20 items to populate queue
        // (currentIndex + 1) is the next item in the list
        val remainingCount = allEpisodes.size - (currentIndex + 1)
        
        android.util.Log.d("SmartQueue", "Play Down List: Current=$currentIndex, Total=${allEpisodes.size}, Remaining=$remainingCount")

        if (remainingCount > 0) {
            val limit = minOf(remainingCount, 20)
            for (i in 1..limit) {
                candidates.add(allEpisodes[currentIndex + i])
            }
        } else {
            // FALLBACK: End of current podcast -> Smart Discovery
            android.util.Log.d("SmartQueue", "End of podcast! Triggering Smart Fallback for genre=${podcast.genre}")
            val fallbackEpisodes = getSmartFallbackEpisodes(podcast)
            android.util.Log.d("SmartQueue", "Fallback returned ${fallbackEpisodes.size} episodes")
            return fallbackEpisodes
        }
        
        android.util.Log.d("SmartQueue", "Returning ${candidates.size} candidates")

        return candidates.map { domainEp ->
            val episodeItem = EpisodeItem(
                id = domainEp.id.toLongOrNull() ?: 0L,
                title = domainEp.title,
                description = domainEp.description,
                enclosureUrl = domainEp.audioUrl,
                duration = domainEp.duration.toInt(),
                datePublished = domainEp.publishedDate,
                image = domainEp.imageUrl,
                feedImage = domainEp.podcastImageUrl
            )
            QueueEntry(episode = episodeItem, podcast = podcast)
        }
    }

    /**
     * Smart Fallback: When the current podcast has no more episodes.
     * Priority:
     * 1. User's subscriptions matching the same genre -> newest unplayed episode
     * 2. Trending podcasts in the same genre -> latest episode
     * Returns QueueEntry with the CORRECT podcast for each episode.
     */
    private suspend fun getSmartFallbackEpisodes(currentPodcast: Podcast): List<QueueEntry> {
        // Fetch fresh podcast details to ensure we have the correct genre
        // (The passed 'currentPodcast' might be from UI or persistence with missing/default genre)
        
        // 1. Try to find in subscriptions first (Fastest & most likely correct for Subs)
        val subscribedPodcasts = subscriptionRepository.subscribedPodcasts.first()
        val cachedPodcast = subscribedPodcasts.find { it.id == currentPodcast.id }
        
        val fullPodcast = if (cachedPodcast != null && !cachedPodcast.genre.isNullOrEmpty()) {
            android.util.Log.d("SmartQueue", "Found podcast in subscriptions: ${cachedPodcast.title}, genre='${cachedPodcast.genre}'")
            cachedPodcast
        } else {
            // 2. If not in subs or missing genre, try network fetch (Slower but necessary for non-subs)
            try {
                android.util.Log.d("SmartQueue", "Fetching full podcast details from network for ${currentPodcast.id}")
                podcastRepository.getPodcastDetails(currentPodcast.id) ?: currentPodcast
            } catch (e: Exception) {
                android.util.Log.w("SmartQueue", "Failed to fetch full podcast details for ${currentPodcast.id}, using provided object", e)
                currentPodcast
            }
        }
        
        val currentGenre = fullPodcast.genre 
        val currentPodcastTitle = fullPodcast.title // For name-based exclusion
        val completedEpisodeIds = listeningHistoryDao.getCompletedEpisodeIds().toSet()
        
        android.util.Log.d("SmartQueue", "Fallback: Genre='$currentGenre' (final), CompletedCount=${completedEpisodeIds.size}, Exclude='$currentPodcastTitle'")
        android.util.Log.d("SmartQueue", "Fallback: User has ${subscribedPodcasts.size} subscriptions")
        
        // DEBUG: Dump all subscription genres
        subscribedPodcasts.forEach { sub ->
            android.util.Log.d("SmartQueue", "  Sub: '${sub.title}' -> genre='${sub.genre}' (expected='$currentGenre')")
        }
        
        // Filter by genre (case-insensitive match on primary genre)
        // Also exclude current podcast by ID AND Title (name match per user request)
        // AND exclude recently played podcasts (loop prevention)
        val limitTimestamp = System.currentTimeMillis() - (12 * 60 * 60 * 1000) // 12 hours ago
        val recentPodcasts = listeningHistoryDao.getRecentlyPlayedPodcasts(limitTimestamp).toSet()
        
        android.util.Log.d("SmartQueue", "Filtering out ${recentPodcasts.size} recently played podcasts: $recentPodcasts")

        val genreMatchingSubs = subscribedPodcasts.filter { sub ->
            sub.id != currentPodcast.id && 
            !sub.title.equals(currentPodcastTitle, ignoreCase = true) &&
            sub.genre.equals(currentGenre, ignoreCase = true) &&
            !recentPodcasts.contains(sub.id) // Loop prevention
        }
        android.util.Log.d("SmartQueue", "Fallback: ${genreMatchingSubs.size} subs match genre '$currentGenre'")

        for (sub in genreMatchingSubs) {
            // Fetch episodes for this subscription
            val subEpisodes = podcastRepository.getEpisodes(sub.id)
                .sortedByDescending { it.publishedDate } // Newest first for discovery
                .filter { it.id !in completedEpisodeIds } // Unplayed (not completed) only
            
            if (subEpisodes.isNotEmpty()) {
                android.util.Log.d("SmartQueue", "Fallback: Found unplayed episode in subscribed pod '${sub.title}'")
                // Return the newest unplayed episode with the SUBSCRIPTION podcast
                val nextEp = subEpisodes.first()
                return listOf(nextEp.toQueueEntry(sub))
            }
        }

        // 2. Fallback to Trending
        android.util.Log.d("SmartQueue", "Fallback: No matching subs with unplayed. Trying Trending for '$currentGenre'")
        val trendingPodcasts = podcastRepository.getTrendingPodcasts(category = currentGenre)
        android.util.Log.d("SmartQueue", "Fallback: Got ${trendingPodcasts.size} trending podcasts for genre")

        for (trendingPod in trendingPodcasts) {
            // Skip current podcast by ID or Title (name match per user request)
            if (trendingPod.id == currentPodcast.id) continue
            if (trendingPod.title.equals(currentPodcastTitle, ignoreCase = true)) continue
            
            // Loop prevention for Trending too!
            if (recentPodcasts.contains(trendingPod.id)) {
                android.util.Log.d("SmartQueue", "Fallback: Skipping trending podcast '${trendingPod.title}' (Recently Played)")
                continue
            }
            
            val trendingEpisodes = podcastRepository.getEpisodes(trendingPod.id)
                .sortedByDescending { it.publishedDate }
                .filter { it.id !in completedEpisodeIds } // Unplayed (not completed) only

            if (trendingEpisodes.isNotEmpty()) {
                android.util.Log.d("SmartQueue", "Fallback: Found trending podcast '${trendingPod.title}' with unplayed")
                val nextEp = trendingEpisodes.first()
                // Return with the TRENDING podcast, not the original!
                return listOf(nextEp.toQueueEntry(trendingPod))
            }
        }

        android.util.Log.w("SmartQueue", "Fallback: No suitable episodes found. Queue will end.")
        return emptyList()
    }

    /**
     * Convert Episode to QueueEntry with its associated podcast.
     */
    private fun Episode.toQueueEntry(podcast: Podcast): QueueEntry {
        val episodeItem = EpisodeItem(
            id = this.id.toLongOrNull() ?: 0L,
            title = this.title,
            description = this.description,
            enclosureUrl = this.audioUrl,
            duration = this.duration.toInt(),
            datePublished = this.publishedDate,
            image = this.imageUrl,
            feedImage = this.podcastImageUrl ?: podcast.imageUrl
        )
        return QueueEntry(episode = episodeItem, podcast = podcast)
    }

    @Deprecated("Use toQueueEntry instead")
    private fun Episode.toEpisodeItem(podcast: Podcast): EpisodeItem {
        return EpisodeItem(
            id = this.id.toLongOrNull() ?: 0L,
            title = this.title,
            description = this.description,
            enclosureUrl = this.audioUrl,
            duration = this.duration.toInt(),
            datePublished = this.publishedDate,
            image = this.imageUrl,
            feedImage = this.podcastImageUrl ?: podcast.imageUrl
        )
    }
}
