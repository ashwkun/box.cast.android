package cx.aswin.boxcast.core.network.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============== TRENDING ==============

@Serializable
data class TrendingResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("feeds")
    val feeds: List<TrendingFeed> = emptyList()
)

@Serializable
data class TrendingFeed(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("author")
    val author: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("artwork")
    val artwork: String? = null,
    
    @SerializedName("language")
    val language: String? = null,
    
    @SerializedName("categories")
    val categories: Map<String, String> = emptyMap(),
    
    @SerializedName("itunesId")
    val itunesId: Long? = null,
    
    @SerializedName("trendScore")
    val trendScore: Int? = null,
    
    @SerializedName("newestItemPublishTime")
    val newestItemPublishTime: Long? = null,
    
    @SerializedName("latestEpisode")
    val latestEpisode: EpisodeItem? = null
)

// ============== SEARCH ==============

@Serializable
data class SearchResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("feeds")
    val feeds: List<SearchFeed> = emptyList()
)

@Serializable
data class SearchFeed(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("author")
    val author: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("artwork")
    val artwork: String? = null,
    
    @SerializedName("categories")
    val categories: Map<String, String> = emptyMap()
)

// ============== EPISODES ==============

@Serializable
data class EpisodesResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("items")
    val items: List<EpisodeItem> = emptyList()
)

@Serializable
data class EpisodesPaginatedResponse(
    @SerializedName("items")
    val items: List<EpisodeItem> = emptyList(),
    
    @SerializedName("hasMore")
    val hasMore: Boolean = false,
    
    @SerializedName("offset")
    val offset: Int = 0,
    
    @SerializedName("limit")
    val limit: Int = 20
)

@Serializable
data class SingleEpisodeResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("episode")
    val episode: EpisodeItem? = null
)

@Serializable
data class EpisodeItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("enclosureUrl")
    val enclosureUrl: String? = null,
    
    @SerializedName("enclosureLength")
    val enclosureLength: Long? = null,
    
    @SerializedName("enclosureType")
    val enclosureType: String? = null,
    
    @SerializedName("duration")
    val duration: Int? = null,
    
    @SerializedName("datePublished")
    val datePublished: Long? = null,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("feedImage")
    val feedImage: String? = null,
    
    @SerializedName("chaptersUrl")
    val chaptersUrl: String? = null,
    
    @SerializedName("transcriptUrl")
    val transcriptUrl: String? = null
)

// ============== SINGLE PODCAST ==============

@Serializable
data class PodcastResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("feed")
    val feed: PodcastFeed? = null
)

@Serializable
data class PodcastFeed(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("url")
    val url: String? = null,
    
    @SerializedName("author")
    val author: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("image")
    val image: String? = null,
    
    @SerializedName("artwork")
    val artwork: String? = null,
    
    @SerializedName("language")
    val language: String? = null,
    
    @SerializedName("categories")
    val categories: Map<String, String> = emptyMap()
)

// ============== METADATA ==============

@Serializable
data class PodcastMetaResponse(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("type")
    val type: String? = "episodic", // serial or episodic
    
    @SerializedName("title")
    val title: String? = null
)
