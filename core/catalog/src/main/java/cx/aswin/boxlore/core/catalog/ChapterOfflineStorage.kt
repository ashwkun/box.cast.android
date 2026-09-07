package cx.aswin.boxlore.core.catalog

import android.content.Context
import android.util.Log
import cx.aswin.boxlore.core.model.Chapter
import java.io.File

/**
 * Manages local file persistence and retrieval of parsed chapter JSON for downloaded episodes.
 */
object ChapterOfflineStorage {

    fun getOfflineChaptersFile(context: Context, episodeId: String): File {
        val dir = File(context.filesDir, "downloaded_chapters")
        return File(dir, "chapter_$episodeId.json")
    }

    fun hasOfflineChapters(context: Context, episodeId: String): Boolean =
        getOfflineChaptersFile(context, episodeId).exists()

    fun getOfflineChapters(context: Context, episodeId: String): List<Chapter> {
        val file = getOfflineChaptersFile(context, episodeId)
        if (!file.exists()) return emptyList()
        return try {
            ChapterRepository.parseChaptersFromJson(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOfflineChapters(context: Context, episodeId: String, json: String): String? = try {
        val dir = File(context.filesDir, "downloaded_chapters").apply { mkdirs() }
        val file = File(dir, "chapter_$episodeId.json")
        file.writeText(json)
        file.absolutePath
    } catch (e: Exception) {
        Log.e("ChapterOfflineStorage", "Failed to save offline chapters for $episodeId", e)
        null
    }

    fun deleteOfflineChapters(context: Context, episodeId: String) {
        try {
            getOfflineChaptersFile(context, episodeId).delete()
        } catch (_: Exception) {
        }
    }
}
