package cx.aswin.boxcast.core.data

import cx.aswin.boxcast.core.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Fetches and parses Podcast 2.0 JSON Chapters from a chaptersUrl.
 * Format: https://github.com/Podcastindex-org/podcast-namespace/blob/main/chapters/jsonChapters.md
 */
object ChapterRepository {
    
    private val cache = mutableMapOf<String, List<Chapter>>()
    
    suspend fun getChapters(chaptersUrl: String): List<Chapter> = withContext(Dispatchers.IO) {
        // Return cached if available
        cache[chaptersUrl]?.let { return@withContext it }
        
        try {
            val json = URL(chaptersUrl).readText()
            val root = JSONObject(json)
            val chaptersArray = root.optJSONArray("chapters") ?: return@withContext emptyList()
            
            val chapters = (0 until chaptersArray.length()).map { i ->
                val obj = chaptersArray.getJSONObject(i)
                Chapter(
                    startTime = obj.optDouble("startTime", 0.0),
                    title = obj.optString("title", "Chapter ${i + 1}"),
                    img = obj.optString("img", null),
                    url = obj.optString("url", null)
                )
            }.sortedBy { it.startTime }
            
            cache[chaptersUrl] = chapters
            chapters
        } catch (e: Exception) {
            android.util.Log.w("ChapterRepo", "Failed to fetch chapters: $chaptersUrl", e)
            emptyList()
        }
    }
    
    fun clearCache() {
        cache.clear()
    }
}
