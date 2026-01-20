package cx.aswin.boxcast.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String? = null,
    val duration: Int = 0, // seconds
    val publishedDate: Long = 0L // Unix timestamp
)
