package cx.aswin.boxcast.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============== TRENDING ==============

@Serializable
data class TrendingResponse(
    val status: String,
    val feeds: List<TrendingFeed> = emptyList()
)

@Serializable
data class TrendingFeed(
    val id: Long,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val image: String? = null,
    val artwork: String? = null,
    val language: String? = null,
    val categories: Map<String, String> = emptyMap(),
    val itunesId: Long? = null,
    val trendScore: Int? = null,
    val newestItemPublishTime: Long? = null,
    val latestEpisode: EpisodeItem? = null
)

// ============== SEARCH ==============

@Serializable
data class SearchResponse(
    val status: String,
    val feeds: List<SearchFeed> = emptyList()
)

@Serializable
data class SearchFeed(
    val id: Long,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val image: String? = null,
    val artwork: String? = null,
    val categories: Map<String, String> = emptyMap()
)

// ============== EPISODES ==============

@Serializable
data class EpisodesResponse(
    val status: String,
    val items: List<EpisodeItem> = emptyList()
)

@Serializable
data class EpisodesPaginatedResponse(
    val items: List<EpisodeItem> = emptyList(),
    val hasMore: Boolean = false,
    val offset: Int = 0,
    val limit: Int = 20
)

@Serializable
data class SingleEpisodeResponse(
    val status: String,
    val episode: EpisodeItem? = null
)

@Serializable
data class EpisodeItem(
    val id: Long,
    val title: String,
    val description: String? = null,
    val enclosureUrl: String? = null,
    val enclosureLength: Long? = null,
    val enclosureType: String? = null,
    val duration: Int? = null,
    val datePublished: Long? = null,
    val image: String? = null,
    val feedImage: String? = null,
    val chaptersUrl: String? = null,
    val transcriptUrl: String? = null
)

// ============== SINGLE PODCAST ==============

@Serializable
data class PodcastResponse(
    val status: String,
    val feed: PodcastFeed? = null
)

@Serializable
data class PodcastFeed(
    val id: Long,
    val title: String,
    val url: String? = null,
    val author: String? = null,
    val description: String? = null,
    val image: String? = null,
    val artwork: String? = null,
    val language: String? = null,
    val categories: Map<String, String> = emptyMap()
)
