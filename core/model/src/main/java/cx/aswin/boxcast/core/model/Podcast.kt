package cx.aswin.boxcast.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val description: String? = null,
    val genre: String = "Podcast",
    val colorHex: String? = null, // For dynamic theming storage
    val fallbackImageUrl: String? = null, // Logic: Episode Art -> Fallback Podcast Art
    val latestEpisode: Episode? = null,
    val resumeProgress: Float? = null // 0.0 - 1.0
)
