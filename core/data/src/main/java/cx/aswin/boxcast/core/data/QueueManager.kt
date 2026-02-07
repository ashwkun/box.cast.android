package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.network.model.EpisodeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor(
    private val queueRepository: QueueRepository,
    private val smartQueueEngine: SmartQueueEngine,
    private val playbackRepository: PlaybackRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isRefilling = false // Prevent multiple refills
    
    init {
        // Set up auto-refill callback
        playbackRepository.queueRefillCallback = { currentEpisode, podcast ->
            refillQueue(currentEpisode, podcast)
        }
    }
    
    /**
     * Auto-refill queue when running low on episodes
     */
    private fun refillQueue(currentEpisode: cx.aswin.boxcast.core.model.Episode, podcast: cx.aswin.boxcast.core.model.Podcast) {
        if (isRefilling) return
        isRefilling = true
        
        scope.launch {
            android.util.Log.d("QueueManager", "Auto-refill triggered for: ${currentEpisode.title}")
            
            // Create EpisodeItem from current episode for SmartQueueEngine
            val currentItem = EpisodeItem(
                id = currentEpisode.id.toLongOrNull() ?: 0L,
                title = currentEpisode.title,
                description = currentEpisode.description,
                enclosureUrl = currentEpisode.audioUrl,
                duration = currentEpisode.duration,
                datePublished = currentEpisode.publishedDate,
                image = currentEpisode.imageUrl,
                feedImage = currentEpisode.podcastImageUrl
            )
            
            // Get more episodes
            val nextEpisodes = smartQueueEngine.getNextEpisodes(currentItem, podcast, null)
            android.util.Log.d("QueueManager", "Auto-refill got ${nextEpisodes.size} more episodes")
            
            nextEpisodes.forEach { nextEp ->
                val domainNext = nextEp.toDomain(podcast)
                
                // Add to Persistence
                queueRepository.addToQueue(nextEp, podcast)
                
                // Add to Active Player Queue
                playbackRepository.addToQueue(domainNext, podcast)
            }
            
            isRefilling = false
        }
    }

    fun playEpisode(episode: EpisodeItem, podcast: cx.aswin.boxcast.core.model.Podcast?, preferredSort: String? = null) {
        scope.launch {
            android.util.Log.d("QueueManager", "playEpisode called: ${episode.title}, sort=$preferredSort")
            
            if (podcast != null) {
                // 1. Clear current queue for a fresh start
                queueRepository.clearQueue()
                
                // 2. Add selected episode (Current playing) & Persist
                queueRepository.addToQueue(episode, podcast)
                
                // 3. Start playback IMMEDIATELY with just the current episode
                val domainEpisode = episode.toDomain(podcast)
                android.util.Log.d("QueueManager", "Starting playback immediately for: ${domainEpisode.title}")
                playbackRepository.playQueue(listOf(domainEpisode), podcast, 0)
                
                // 4. Smart Auto-fill (Up Next) in Background
                // This network call won't block the UI/Player from starting
                val nextEpisodes = smartQueueEngine.getNextEpisodes(episode, podcast, preferredSort)
                android.util.Log.d("QueueManager", "SmartEngine returned ${nextEpisodes.size} next episodes")
                
                nextEpisodes.forEach { nextEp ->
                    val domainNext = nextEp.toDomain(podcast)
                    
                    // Add to Persistence
                    queueRepository.addToQueue(nextEp, podcast)
                    
                    // Add to Active Player Queue
                    playbackRepository.addToQueue(domainNext, podcast)
                }
            } else {
                 android.util.Log.e("QueueManager", "Podcast is null!")
            }
        }
    }

    fun addToQueue(episode: EpisodeItem, podcast: cx.aswin.boxcast.core.model.Podcast?) {
        scope.launch {
            if (podcast != null) {
                // Persist
                queueRepository.addToQueue(episode, podcast)
                
                // Add to Player
                val domainEpisode = episode.toDomain(podcast)
                playbackRepository.addToQueue(domainEpisode, podcast)
            }
        }
    }
    
    // Overload for Domain Objects (UI)
    fun playEpisode(episode: cx.aswin.boxcast.core.model.Episode, podcast: cx.aswin.boxcast.core.model.Podcast?, preferredSort: String? = null) {
        val currentQueue = playbackRepository.playerState.value.queue
        val currentPodcast = playbackRepository.playerState.value.currentPodcast

        // Check if we can just skip to the episode in the existing queue
        if (podcast != null && currentPodcast?.id == podcast.id) {
             val index = currentQueue.indexOfFirst { it.id == episode.id }
             if (index != -1) {
                 android.util.Log.d("QueueManager", "Episode found in existing queue at index $index. Skipping to it.")
                 playbackRepository.skipToEpisode(index)
                 return
             }
        }

        val item = episode.toEpisodeItem(podcast)
        playEpisode(item, podcast, preferredSort)
    }

    private fun cx.aswin.boxcast.core.model.Episode.toEpisodeItem(podcast: cx.aswin.boxcast.core.model.Podcast?): EpisodeItem {
        return EpisodeItem(
            id = this.id.toLongOrNull() ?: 0L,
            title = this.title,
            description = this.description,
            enclosureUrl = this.audioUrl,
            duration = this.duration,
            datePublished = this.publishedDate,
            image = this.imageUrl,
            feedImage = this.podcastImageUrl ?: podcast?.imageUrl
        )
    }

    private fun EpisodeItem.toDomain(podcast: cx.aswin.boxcast.core.model.Podcast): cx.aswin.boxcast.core.model.Episode {
        return cx.aswin.boxcast.core.model.Episode(
            id = this.id.toString(),
            title = this.title,
            description = this.description ?: "",
            audioUrl = this.enclosureUrl ?: "",
            imageUrl = (this.image?.takeIf { it.isNotBlank() } ?: this.feedImage?.takeIf { it.isNotBlank() }) ?: podcast.imageUrl,
            podcastImageUrl = this.feedImage?.takeIf { it.isNotBlank() } ?: podcast.imageUrl, // Ensure fallback
            podcastTitle = podcast.title,
            duration = this.duration ?: 0,
            publishedDate = this.datePublished ?: 0L
        )
    }
}
