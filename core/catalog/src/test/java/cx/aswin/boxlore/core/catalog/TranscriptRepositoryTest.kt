package cx.aswin.boxlore.core.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TranscriptRepositoryTest {
    @Test
    fun testParseVtt_stripsHtmlAndVttTags() {
        val vttContent =
            """
            WEBVTT

            00:00:01.000 --> 00:00:04.000
            <v Host>Welcome to the <i>podcast</i>!</v>

            00:00:04.500 --> 00:00:08.000
            <c.yellow>Thank you for having me.</c>
            """.trimIndent()

        val segments = TranscriptRepository.parseVtt(vttContent)

        assertEquals(2, segments.size)
        assertEquals(1000L, segments[0].startMs)
        assertEquals(4000L, segments[0].endMs)
        assertEquals("Welcome to the podcast!", segments[0].text)

        assertEquals(4500L, segments[1].startMs)
        assertEquals(8000L, segments[1].endMs)
        assertEquals("Thank you for having me.", segments[1].text)
    }

    @Test
    fun testParseSrt_stripsHtmlTags() {
        val srtContent =
            """
            1
            00:00:01,000 --> 00:00:04,000
            Hello <b>world</b>!

            2
            00:00:05,000 --> 00:00:09,000
            This is a <font color="red">test</font>.
            """.trimIndent()

        val segments = TranscriptRepository.parseSrt(srtContent)

        assertEquals(2, segments.size)
        assertEquals(1000L, segments[0].startMs)
        assertEquals(4000L, segments[0].endMs)
        assertEquals("Hello world!", segments[0].text)

        assertEquals(5000L, segments[1].startMs)
        assertEquals(9000L, segments[1].endMs)
        assertEquals("This is a test.", segments[1].text)
    }

    @Test
    fun testParseVtt_timelineHealing() {
        // Test timeline healing where end time is before start time or non-chronological
        val vttContent =
            """
            WEBVTT

            00:00:05.000 --> 00:00:02.000
            Malformed timing segment
            """.trimIndent()

        val segments = TranscriptRepository.parseVtt(vttContent)

        assertEquals(1, segments.size)
        assertTrue(segments[0].startMs < segments[0].endMs, "Start time should be before end time after healing")
    }

    @Test
    fun testOfflineTranscriptLifecycle() {
        val tempDir = java.nio.file.Files.createTempDirectory("tr_test").toFile()
        try {
            val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
            org.mockito.Mockito.`when`(mockContext.filesDir).thenReturn(tempDir)

            val episodeId = "ep_trans_456"
            org.junit.jupiter.api.Assertions.assertFalse(TranscriptOfflineStorage.hasOfflineTranscript(mockContext, episodeId))
            assertTrue(TranscriptOfflineStorage.getOfflineTranscript(mockContext, episodeId).isEmpty())

            val srt = """
                1
                00:00:01,000 --> 00:00:03,000
                Offline subtitle test.
            """.trimIndent()
            val path = TranscriptOfflineStorage.saveOfflineTranscript(mockContext, episodeId, srt)

            org.junit.jupiter.api.Assertions.assertNotNull(path)
            assertTrue(TranscriptOfflineStorage.hasOfflineTranscript(mockContext, episodeId))

            val loaded = TranscriptOfflineStorage.getOfflineTranscript(mockContext, episodeId)
            assertEquals(1, loaded.size)
            assertEquals("Offline subtitle test.", loaded[0].text)

            TranscriptOfflineStorage.deleteOfflineTranscript(mockContext, episodeId)
            org.junit.jupiter.api.Assertions.assertFalse(TranscriptOfflineStorage.hasOfflineTranscript(mockContext, episodeId))
            assertTrue(TranscriptOfflineStorage.getOfflineTranscript(mockContext, episodeId).isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testGetTranscriptFromLocalFile() = kotlinx.coroutines.test.runTest {
        val tempFile = java.io.File.createTempFile("transcript", ".srt")
        try {
            val srt = """
                1
                00:00:02,000 --> 00:00:05,000
                Local file subtitle.
            """.trimIndent()
            tempFile.writeText(srt)

            val fromPath = TranscriptRepository.getTranscript(tempFile.absolutePath)
            assertEquals(1, fromPath.size)
            assertEquals("Local file subtitle.", fromPath[0].text)

            val fromUri = TranscriptRepository.getTranscript("file://${tempFile.absolutePath}")
            assertEquals(1, fromUri.size)
            assertEquals("Local file subtitle.", fromUri[0].text)
        } finally {
            tempFile.delete()
        }
    }
}
