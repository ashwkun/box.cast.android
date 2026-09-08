package cx.aswin.boxlore.core.catalog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cx.aswin.boxlore.core.model.Chapter
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ChapterRepository.clearCache()
    }

    @After
    fun tearDown() {
        ChapterRepository.clearCache()
    }

    // ---- Cache ----

    @Test
    fun cacheRoundTripsAndClears() {
        val chapters = listOf(Chapter(startTime = 0.0, title = "Intro"))
        ChapterRepository.setCachedChapters("k", chapters)

        assertSame(chapters, ChapterRepository.getCachedChapters("k"))
        ChapterRepository.clearCache()
        assertNull(ChapterRepository.getCachedChapters("k"))
    }

    // ---- parseChaptersFromDescription ----

    @Test
    fun parseReturnsEmptyForNullOrBlank() {
        assertTrue(ChapterRepository.parseChaptersFromDescription(null).isEmpty())
        assertTrue(ChapterRepository.parseChaptersFromDescription("").isEmpty())
    }

    @Test
    fun parseRequiresAtLeastTwoTimestampsToAvoidFalsePositives() {
        val single = ChapterRepository.parseChaptersFromDescription("<p>00:00 Only Intro</p>")
        assertTrue(single.isEmpty())
    }

    @Test
    fun parseExtractsHtmlSeparatedTimestampsSortedByStartTime() {
        val html = "<p>01:30 Second</p><p>00:00 Intro</p><br/>1:02:03 Finale"

        val chapters = ChapterRepository.parseChaptersFromDescription(html)

        assertEquals(listOf("Intro", "Second", "Finale"), chapters.map { it.title })
        assertEquals(listOf(0.0, 90.0, 3723.0), chapters.map { it.startTime })
    }

    @Test
    fun parseHandlesTitleBeforeTimestamp() {
        val html = "Introduction 00:00\nChapter Two 05:00"

        val chapters = ChapterRepository.parseChaptersFromDescription(html)

        assertEquals(listOf("Introduction", "Chapter Two"), chapters.map { it.title })
    }

    @Test
    fun parseSkipsInvalidMinuteAndSecondFields() {
        // 00:60 has 60 seconds (invalid), 99:99 minutes/seconds out of range — both dropped,
        // leaving fewer than two valid entries so the guard yields an empty list.
        val html = "<p>00:60 Bad Seconds</p><p>99:99 Bad Both</p>"

        assertTrue(ChapterRepository.parseChaptersFromDescription(html).isEmpty())
    }

    @Test
    fun parseStripsSurroundingPunctuationFromTitles() {
        val html = "<li>00:00 - Intro:</li><li>02:15 :: Deep Dive --</li>"

        val chapters = ChapterRepository.parseChaptersFromDescription(html)

        assertEquals(listOf("Intro", "Deep Dive"), chapters.map { it.title })
    }

    @Test
    fun parseDropsTimestampsWithoutTitles() {
        // Bare timestamps with no accompanying text produce empty titles and are ignored,
        // so with only one real chapter the >= 2 guard returns empty.
        val html = "<p>00:00</p><p>01:00 Real Chapter</p>"

        assertTrue(ChapterRepository.parseChaptersFromDescription(html).isEmpty())
    }

    // ---- JSON round-trip & offline helpers ----

    @Test
    fun parseChaptersFromJsonAndChaptersToJsonRoundTrip() {
        val chapters = listOf(
            Chapter(title = "Intro", startTime = 0.0),
            Chapter(title = "Discussion", startTime = 30.0),
        )
        val json = ChapterRepository.chaptersToJson(chapters)
        val parsed = ChapterRepository.parseChaptersFromJson(json)

        assertEquals(2, parsed.size)
        assertEquals("Intro", parsed[0].title)
        assertEquals(0.0, parsed[0].startTime, 0.001)
        assertEquals("Discussion", parsed[1].title)
        assertEquals(30.0, parsed[1].startTime, 0.001)
    }

    @Test
    fun hasChaptersInDescriptionReturnsTrueOnlyWhenTwoOrMoreTimestamps() {
        assertTrue(ChapterRepository.hasChaptersInDescription("<p>00:00 Intro</p><p>02:00 Main</p>"))
        assertFalse(ChapterRepository.hasChaptersInDescription("<p>00:00 Intro</p>"))
        assertFalse(ChapterRepository.hasChaptersInDescription(null))
        assertFalse(ChapterRepository.hasChaptersInDescription(""))
    }

    @Test
    fun offlineChaptersSaveGetAndDeleteLifecycle() {
        val episodeId = "ep_test_123"
        assertFalse(ChapterOfflineStorage.hasOfflineChapters(context, episodeId))
        assertTrue(ChapterOfflineStorage.getOfflineChapters(context, episodeId).isEmpty())

        val chapters = listOf(
            Chapter(title = "Chapter 1", startTime = 0.0),
            Chapter(title = "Chapter 2", startTime = 60.0),
        )
        val json = ChapterRepository.chaptersToJson(chapters)
        val path = ChapterOfflineStorage.saveOfflineChapters(context, episodeId, json)

        assertNotNull(path)
        assertTrue(ChapterOfflineStorage.hasOfflineChapters(context, episodeId))

        val loaded = ChapterOfflineStorage.getOfflineChapters(context, episodeId)
        assertEquals(2, loaded.size)
        assertEquals("Chapter 1", loaded[0].title)
        assertEquals("Chapter 2", loaded[1].title)

        ChapterOfflineStorage.deleteOfflineChapters(context, episodeId)
        assertFalse(ChapterOfflineStorage.hasOfflineChapters(context, episodeId))
        assertTrue(ChapterOfflineStorage.getOfflineChapters(context, episodeId).isEmpty())
    }

    @Test
    fun getChaptersFromLocalFilePathParsesCorrectly() = runTest {
        val tempFile = File.createTempFile("chapters", ".json")
        try {
            val json = """
                {
                    "version": "1.2.0",
                    "chapters": [
                        { "title": "Local Intro", "startTime": 0.0 },
                        { "title": "Local Outro", "startTime": 100.0 }
                    ]
                }
            """.trimIndent()
            tempFile.writeText(json)

            val chapters = ChapterRepository.getChapters(tempFile.absolutePath)
            assertEquals(2, chapters.size)
            assertEquals("Local Intro", chapters[0].title)
            assertEquals(0.0, chapters[0].startTime, 0.001)
            assertEquals("Local Outro", chapters[1].title)
            assertEquals(100.0, chapters[1].startTime, 0.001)

            val fileUriChapters = ChapterRepository.getChapters("file://${tempFile.absolutePath}")
            assertEquals(2, fileUriChapters.size)
            assertEquals("Local Intro", fileUriChapters[0].title)
        } finally {
            tempFile.delete()
        }
    }
}
