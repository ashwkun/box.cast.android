package cx.aswin.boxcast.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val ids: List<String>
)

@Serializable
data class SyncResponse(
    val items: List<SyncItem> = emptyList(),
    val error: String? = null
)

@Serializable
data class SyncItem(
    val id: String,
    val latestEpisode: EpisodeItem? = null
)
