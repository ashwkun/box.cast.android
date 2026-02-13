package cx.aswin.boxcast.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Parsed transcript segment with timing.
 */
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/**
 * Fetches and parses podcast transcripts in SRT or VTT format.
 */
object TranscriptRepository {
    
    private val cache = mutableMapOf<String, List<TranscriptSegment>>()
    
    suspend fun getTranscript(url: String, type: String? = null): List<TranscriptSegment> = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        
        try {
            val content = URL(url).readText()
            val segments = when {
                type?.contains("srt") == true || url.endsWith(".srt", ignoreCase = true) -> parseSrt(content)
                type?.contains("vtt") == true || url.endsWith(".vtt", ignoreCase = true) -> parseVtt(content)
                content.trimStart().startsWith("WEBVTT") -> parseVtt(content)
                else -> parseSrt(content) // Default to SRT
            }
            cache[url] = segments
            segments
        } catch (e: Exception) {
            android.util.Log.w("TranscriptRepo", "Failed to fetch transcript: $url", e)
            emptyList()
        }
    }
    
    /**
     * Parse SRT format:
     * 1
     * 00:00:01,000 --> 00:00:04,000
     * Hello and welcome
     */
    private fun parseSrt(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val blocks = content.trim().split(Regex("\\n\\n+"))
        
        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.size < 3) continue
            
            // Line 1: sequence number (skip)
            // Line 2: timestamps
            val timeLine = lines[1]
            val times = parseTimestampLine(timeLine) ?: continue
            
            // Lines 3+: text
            val text = lines.drop(2).joinToString(" ").trim()
                .replace(Regex("<[^>]+>"), "") // Strip HTML tags
            
            if (text.isNotBlank()) {
                segments.add(TranscriptSegment(startMs = times.first, endMs = times.second, text = text))
            }
        }
        return segments
    }
    
    /**
     * Parse WebVTT format:
     * WEBVTT
     *
     * 00:00:01.000 --> 00:00:04.000
     * Hello and welcome
     */
    private fun parseVtt(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val blocks = content.trim().split(Regex("\\n\\n+"))
        
        for (block in blocks) {
            val lines = block.trim().lines()
            
            // Find the line with -->
            val timeLineIdx = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIdx < 0) continue
            
            val times = parseTimestampLine(lines[timeLineIdx]) ?: continue
            val text = lines.drop(timeLineIdx + 1).joinToString(" ").trim()
                .replace(Regex("<[^>]+>"), "") // Strip VTT tags (<v>, <c>, etc.)
            
            if (text.isNotBlank()) {
                segments.add(TranscriptSegment(startMs = times.first, endMs = times.second, text = text))
            }
        }
        return segments
    }
    
    /**
     * Parse "00:01:23,456 --> 00:01:26,789" or "00:01:23.456 --> 00:01:26.789"
     */
    private fun parseTimestampLine(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")
        if (parts.size != 2) return null
        val start = parseTimestamp(parts[0].trim()) ?: return null
        val end = parseTimestamp(parts[1].trim().split(" ").first()) ?: return null
        return Pair(start, end)
    }
    
    /**
     * Parse "HH:MM:SS,mmm" or "HH:MM:SS.mmm" or "MM:SS.mmm" to milliseconds
     */
    private fun parseTimestamp(ts: String): Long? {
        val cleaned = ts.replace(",", ".")
        val parts = cleaned.split(":")
        return try {
            when (parts.size) {
                3 -> {
                    val h = parts[0].toLong()
                    val m = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    val s = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0
                    h * 3600000 + m * 60000 + s * 1000 + ms
                }
                2 -> {
                    val m = parts[0].toLong()
                    val secParts = parts[1].split(".")
                    val s = secParts[0].toLong()
                    val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0
                    m * 60000 + s * 1000 + ms
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    fun clearCache() {
        cache.clear()
    }
}
