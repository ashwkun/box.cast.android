package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.network.BoxCastApi
import cx.aswin.boxcast.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import okhttp3.ResponseBody
import java.io.InputStreamReader

import cx.aswin.boxcast.core.network.model.TrendingFeed

/**
 * Repository for podcast data via BoxCast API (Cloudflare Worker → Podcast Index)
 */
class PodcastRepository(
    private val baseUrl: String,
    private val apiKey: String,
    context: android.content.Context
) {
    private val api: BoxCastApi = NetworkModule.createBoxCastApi(baseUrl, context)

    suspend fun getTrendingPodcasts(country: String = "us", limit: Int = 50, category: String? = null): List<Podcast> {
        // Fallback or non-streaming implementation
        return try {
            android.util.Log.d("BoxCastRepo", "Fetching category: $category for country: $country")
            val response = api.getTrending(apiKey, country, limit, category)
            android.util.Log.d("BoxCastRepo", "Category response count: ${response.feeds.size}, First: ${response.feeds.firstOrNull()?.title}")
            mapFeedsToPodcasts(response.feeds)
        } catch (e: Exception) {
            android.util.Log.e("BoxCastRepo", "Category fetch failed for $category", e)
            emptyList()
        }
    }

    fun getTrendingPodcastsStream(country: String = "us", limit: Int = 50, category: String? = null): kotlinx.coroutines.flow.Flow<List<Podcast>> = kotlinx.coroutines.flow.flow {
        val podcasts = mutableListOf<Podcast>()
        val startTime = System.currentTimeMillis()
        android.util.Log.d("BoxCastTiming", "Repo: Requesting stream... cat=$category")
        try {
            val responseBody = api.getTrendingStream(apiKey, country, limit, category)
            android.util.Log.d("BoxCastTiming", "Repo: Response headers received in ${System.currentTimeMillis() - startTime}ms")
            
            val stream = responseBody.byteStream()
            val reader = com.google.gson.stream.JsonReader(java.io.InputStreamReader(stream, "UTF-8"))
            
            reader.beginObject() // {
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "feeds") {
                    reader.beginArray() // [
                    while (reader.hasNext()) {
                        // Parse one feed object
                        val feed = com.google.gson.Gson().fromJson<cx.aswin.boxcast.core.network.model.TrendingFeed>(
                            reader, 
                            cx.aswin.boxcast.core.network.model.TrendingFeed::class.java
                        )
                        
                        val podcast = Podcast(
                            id = feed.id.toString(),
                            title = feed.title,
                            artist = feed.author ?: "Unknown",
                            imageUrl = feed.artwork ?: feed.image ?: "",
                            description = feed.description,
                            genre = feed.categories.values.firstOrNull() ?: "Podcast",
                            latestEpisode = feed.latestEpisode?.let { mapToEpisode(it) }
                        )
                        podcasts.add(podcast)
                        // android.util.Log.d("BoxCastStream", "Parsed item #${podcasts.size}: ${podcast.title}")
                        
                        // PROGRESSIVE EMISSION STRATEGY
                        // Emit every 4 items to ensure UI updates frequently
                        if (podcasts.size % 4 == 0 || podcasts.size == 2) {
                             // android.util.Log.d("BoxCastStream", "Emitting ${podcasts.size} items")
                             emit(podcasts.toList())
                        }
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            
            // Final emission
            if (podcasts.isNotEmpty()) {
                android.util.Log.d("BoxCastStream", "Stream Complete. Total: ${podcasts.size}")
                emit(podcasts)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BoxCastStream", "Stream Error after ${podcasts.size} items", e)
            if (podcasts.isNotEmpty()) emit(podcasts)
        }
    }.flowOn(Dispatchers.IO)

    private fun mapFeedsToPodcasts(feeds: List<cx.aswin.boxcast.core.network.model.TrendingFeed>): List<Podcast> {
        return feeds.map { feed ->
            Podcast(
                id = feed.id.toString(),
                title = feed.title,
                artist = feed.author ?: "Unknown",
                imageUrl = feed.artwork ?: feed.image ?: "",
                description = feed.description,
                genre = feed.categories.values.firstOrNull() ?: "Podcast",
                latestEpisode = feed.latestEpisode?.let { mapToEpisode(it) }
            )
        }
    }

    suspend fun searchPodcasts(query: String): List<Podcast> = withContext(Dispatchers.IO) {
        try {
            val response = api.search(apiKey, query)
            response.feeds.map { feed ->
                Podcast(
                    id = feed.id.toString(),
                    title = feed.title,
                    artist = feed.author ?: "Unknown",
                    imageUrl = feed.artwork ?: feed.image ?: "",
                    description = feed.description,
                    genre = feed.categories.values.firstOrNull() ?: "Podcast"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getEpisodes(feedId: String): List<Episode> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEpisodes(apiKey, feedId)
            response.items.mapNotNull { mapToEpisode(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getEpisode(episodeId: String): Episode? = withContext(Dispatchers.IO) {
        try {
            val response = api.getEpisode(apiKey, episodeId)
            response.episode?.let { mapToEpisode(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class EpisodePage(
        val episodes: List<Episode>,
        val hasMore: Boolean
    )

    suspend fun getEpisodesPaginated(
        feedId: String,
        limit: Int = 20,
        offset: Int = 0,
        sort: String = "newest"
    ): EpisodePage = withContext(Dispatchers.IO) {
        try {
            val response = api.getEpisodesPaginated(apiKey, feedId, limit, offset, sort)
            EpisodePage(
                episodes = response.items.mapNotNull { mapToEpisode(it) },
                hasMore = response.hasMore
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EpisodePage(emptyList(), false)
        }
    }

    suspend fun getPodcastDetails(feedId: String): Podcast? = withContext(Dispatchers.IO) {
        try {
            val response = api.getPodcast(apiKey, feedId)
            val feed = response.feed ?: return@withContext null
            Podcast(
                id = feed.id.toString(),
                title = feed.title,
                artist = feed.author ?: "Unknown",
                imageUrl = feed.artwork ?: feed.image ?: "",
                description = feed.description,
                genre = feed.categories.values.firstOrNull() ?: "Podcast"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun syncSubscriptions(feedIds: List<String>): Map<String, Episode> = withContext(Dispatchers.IO) {
        try {
            if (feedIds.isEmpty()) return@withContext emptyMap()
            
            // Chunking done on proxy side mostly, but safe to chunk here if needed (e.g. > 20)
            // For now, pass all, assuming Proxy handles/truncates or client logic limits it.
            // Actually, we should chunk here to be safe if list is huge? 
            // The logic: Let's assume VM sends reasonable amount or we chunk 20.
            // Better to let VM handle logic of *what* to sync, Repo just syncs.
            
            val request = cx.aswin.boxcast.core.network.model.SyncRequest(feedIds)
            val response = api.syncSubscriptions(apiKey, request)
            
            response.items.mapNotNull { item ->
                val ep = item.latestEpisode?.let { mapToEpisode(it) }
                if (ep != null) item.id to ep else null
            }.toMap()
            
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
    
    private fun mapToEpisode(item: cx.aswin.boxcast.core.network.model.EpisodeItem): Episode? {
        val audioUrl = item.enclosureUrl ?: return null
        return Episode(
            id = item.id.toString(),
            title = item.title,
            description = item.description ?: "",
            audioUrl = audioUrl,
            imageUrl = item.image ?: item.feedImage ?: "",
            podcastImageUrl = item.feedImage,
            duration = item.duration ?: 0,
            publishedDate = item.datePublished ?: 0L
        )
    }
}
