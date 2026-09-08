package cx.aswin.boxlore.core.catalog

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages local file persistence and retrieval of raw transcripts for downloaded episodes.
 */
object TranscriptOfflineStorage {

    fun getOfflineTranscriptFile(context: Context, episodeId: String): File {
        val dir = File(context.filesDir, "downloaded_transcripts")
        return File(dir, "transcript_$episodeId.txt")
    }

    fun hasOfflineTranscript(context: Context, episodeId: String): Boolean =
        getOfflineTranscriptFile(context, episodeId).exists()

    fun getOfflineTranscript(context: Context, episodeId: String): List<TranscriptSegment> {
        val file = getOfflineTranscriptFile(context, episodeId)
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            if (content.trimStart().startsWith("WEBVTT")) {
                TranscriptRepository.parseVtt(content)
            } else {
                TranscriptRepository.parseSrt(content)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOfflineTranscript(context: Context, episodeId: String, content: String): String? = try {
        val dir = File(context.filesDir, "downloaded_transcripts").apply { mkdirs() }
        val file = File(dir, "transcript_$episodeId.txt")
        file.writeText(content)
        file.absolutePath
    } catch (e: Exception) {
        Log.e("TranscriptOfflineStorage", "Failed to save offline transcript for $episodeId", e)
        null
    }

    fun deleteOfflineTranscript(context: Context, episodeId: String) {
        try {
            getOfflineTranscriptFile(context, episodeId).delete()
        } catch (_: Exception) {
        }
    }
}
