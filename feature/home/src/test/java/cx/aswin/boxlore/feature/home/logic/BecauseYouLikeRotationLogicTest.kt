package cx.aswin.boxlore.feature.home.logic

import cx.aswin.boxlore.core.model.PodcastGenres
import cx.aswin.boxlore.core.testing.TestFixtures
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BecauseYouLikeRotationLogicTest {

    @Test
    fun `resolveDayPart correctly maps all 24 hours`() {
        // Night: 20:00 - 04:59
        listOf(0, 1, 2, 3, 4).forEach { hour ->
            assertEquals(BecauseYouLikeRotationLogic.DayPart.NIGHT, BecauseYouLikeRotationLogic.resolveDayPart(hour))
        }
        // Morning: 05:00 - 11:59
        listOf(5, 6, 7, 8, 9, 10, 11).forEach { hour ->
            assertEquals(BecauseYouLikeRotationLogic.DayPart.MORNING, BecauseYouLikeRotationLogic.resolveDayPart(hour))
        }
        // Afternoon: 12:00 - 19:59
        listOf(12, 13, 14, 15, 16, 17, 18, 19).forEach { hour ->
            assertEquals(BecauseYouLikeRotationLogic.DayPart.AFTERNOON, BecauseYouLikeRotationLogic.resolveDayPart(hour))
        }
        // Night: 20:00 - 23:59
        listOf(20, 21, 22, 23).forEach { hour ->
            assertEquals(BecauseYouLikeRotationLogic.DayPart.NIGHT, BecauseYouLikeRotationLogic.resolveDayPart(hour))
        }
    }

    @Test
    fun `computeSlotKey and currentSlotKey format correctly with fixed clock`() {
        val fixedClock = Clock.fixed(
            Instant.parse("2026-09-08T09:30:00Z"),
            ZoneId.of("UTC"),
        )
        // 2026-09-08 at 09:30 UTC is morning
        val slotKey = BecauseYouLikeRotationLogic.currentSlotKey(fixedClock)
        assertTrue(slotKey.endsWith(":MORNING"))

        val manualKey = BecauseYouLikeRotationLogic.computeSlotKey(
            epochDay = 20674,
            dayPart = BecauseYouLikeRotationLogic.DayPart.AFTERNOON,
        )
        assertEquals("20674:AFTERNOON", manualKey)
    }

    @Test
    fun `matchesDayPart matches morning genres and custom genre overrides`() {
        val techPod = TestFixtures.podcast(id = "tech", title = "Tech News").copy(genre = PodcastGenres.TECHNOLOGY)
        val newsPod = TestFixtures.podcast(id = "news", title = "Daily News").copy(genre = PodcastGenres.NEWS)
        val comedyPod = TestFixtures.podcast(id = "comedy", title = "Laughs").copy(genre = PodcastGenres.COMEDY)

        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(techPod, BecauseYouLikeRotationLogic.DayPart.MORNING))
        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(newsPod, BecauseYouLikeRotationLogic.DayPart.MORNING))
        assertFalse(BecauseYouLikeRotationLogic.matchesDayPart(comedyPod, BecauseYouLikeRotationLogic.DayPart.MORNING))

        // Custom genre overrides default genre
        val comedyWithNewsCustom = comedyPod.copy(customGenre = PodcastGenres.NEWS)
        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(comedyWithNewsCustom, BecauseYouLikeRotationLogic.DayPart.MORNING))
    }

    @Test
    fun `matchesDayPart matches afternoon genres and keywords`() {
        val sportsPod = TestFixtures.podcast(id = "sports", title = "Sports Daily").copy(genre = PodcastGenres.SPORTS)
        val culturePod = TestFixtures.podcast(id = "culture", title = "Culture").copy(genre = PodcastGenres.SOCIETY_AND_CULTURE)
        val crimePod = TestFixtures.podcast(id = "crime", title = "Murder").copy(genre = PodcastGenres.TRUE_CRIME)

        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(sportsPod, BecauseYouLikeRotationLogic.DayPart.AFTERNOON))
        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(culturePod, BecauseYouLikeRotationLogic.DayPart.AFTERNOON))
        assertFalse(BecauseYouLikeRotationLogic.matchesDayPart(crimePod, BecauseYouLikeRotationLogic.DayPart.AFTERNOON))
    }

    @Test
    fun `matchesDayPart matches night genres`() {
        val crimePod = TestFixtures.podcast(id = "crime", title = "Casefile").copy(genre = PodcastGenres.TRUE_CRIME)
        val historyPod = TestFixtures.podcast(id = "hist", title = "History of Rome").copy(genre = PodcastGenres.HISTORY)
        val techPod = TestFixtures.podcast(id = "tech", title = "Tech").copy(genre = PodcastGenres.TECHNOLOGY)

        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(crimePod, BecauseYouLikeRotationLogic.DayPart.NIGHT))
        assertTrue(BecauseYouLikeRotationLogic.matchesDayPart(historyPod, BecauseYouLikeRotationLogic.DayPart.NIGHT))
        assertFalse(BecauseYouLikeRotationLogic.matchesDayPart(techPod, BecauseYouLikeRotationLogic.DayPart.NIGHT))
    }

    @Test
    fun `filterEligibleCandidates applies threshold and orders by score then recency`() {
        val scores = mapOf(
            "sub-top" to 200,
            "sub-mid" to 120,
            "sub-low" to 80,
            "unplayed" to 10,
        )
        val lastPlayed = mapOf(
            "sub-top" to 100L,
            "sub-mid" to 200L,
            "sub-low" to 50L,
            "unplayed" to 10L,
        )

        // Threshold = maxOf(15, 200 * 0.5) = 100. sub-low (80) and unplayed (10) are filtered out.
        val candidates = BecauseYouLikeRotationLogic.filterEligibleCandidates(
            scores = scores,
            lastPlayedMap = lastPlayed,
            thresholdRatio = 0.5,
        )

        assertEquals(listOf("sub-top", "sub-mid"), candidates)
    }

    @Test
    fun `filterEligibleCandidates returns empty when scores are empty or below minScore`() {
        assertTrue(BecauseYouLikeRotationLogic.filterEligibleCandidates(emptyMap(), emptyMap()).isEmpty())
        assertTrue(
            BecauseYouLikeRotationLogic.filterEligibleCandidates(
                scores = mapOf("low" to 10),
                lastPlayedMap = emptyMap(),
                minScore = 15,
            ).isEmpty(),
        )
    }

    @Test
    fun `selectRotatedAnchor returns null for empty candidates and single candidate for size 1`() {
        assertNull(
            BecauseYouLikeRotationLogic.selectRotatedAnchor(
                candidates = emptyList(),
                dayPart = BecauseYouLikeRotationLogic.DayPart.MORNING,
                epochDay = 100L,
            ),
        )

        val solo = TestFixtures.podcast(id = "solo", title = "Solo")
        assertEquals(
            solo,
            BecauseYouLikeRotationLogic.selectRotatedAnchor(
                candidates = listOf(solo),
                dayPart = BecauseYouLikeRotationLogic.DayPart.MORNING,
                epochDay = 100L,
            ),
        )
    }

    @Test
    fun `selectRotatedAnchor creates cross-day variation with multiple matching shows`() {
        val morningA = TestFixtures.podcast(id = "m-a", title = "Morning News").copy(genre = PodcastGenres.NEWS)
        val morningB = TestFixtures.podcast(id = "m-b", title = "Tech Morning").copy(genre = PodcastGenres.TECHNOLOGY)
        val other = TestFixtures.podcast(id = "other", title = "Other").copy(genre = PodcastGenres.COMEDY)

        val candidates = listOf(morningA, morningB, other)

        val day0 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 100L)
        val day1 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 101L)
        val day2 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 102L)

        assertEquals(morningA.id, day0?.id)
        assertEquals(morningB.id, day1?.id)
        assertEquals(morningA.id, day2?.id)
    }

    @Test
    fun `selectRotatedAnchor avoids repeating same show when exactly 1 show matches genre`() {
        val morningOnly = TestFixtures.podcast(id = "m-single", title = "Morning News").copy(genre = PodcastGenres.NEWS)
        val comedyA = TestFixtures.podcast(id = "c-a", title = "Comedy A").copy(genre = PodcastGenres.COMEDY)
        val comedyB = TestFixtures.podcast(id = "c-b", title = "Comedy B").copy(genre = PodcastGenres.COMEDY)

        val candidates = listOf(morningOnly, comedyA, comedyB)

        // Day 100 (even): picks the genre match
        val day0 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 100L)
        assertEquals(morningOnly.id, day0?.id)

        // Day 101 (odd): alternates to other candidate in pool for day-to-day variation
        val day1 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 101L)
        assertTrue(day1?.id != morningOnly.id)

        // Day 102 (even): picks genre match again
        val day2 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 102L)
        assertEquals(morningOnly.id, day2?.id)

        // Day 103 (odd): alternates to next candidate in pool
        val day3 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, epochDay = 103L)
        assertTrue(day3?.id != morningOnly.id)
    }

    @Test
    fun `selectRotatedAnchor varies across days and slots when no shows match genre`() {
        val showA = TestFixtures.podcast(id = "a", title = "Show A").copy(genre = PodcastGenres.RELIGION_AND_SPIRITUALITY)
        val showB = TestFixtures.podcast(id = "b", title = "Show B").copy(genre = PodcastGenres.RELIGION_AND_SPIRITUALITY)
        val showC = TestFixtures.podcast(id = "c", title = "Show C").copy(genre = PodcastGenres.RELIGION_AND_SPIRITUALITY)

        val candidates = listOf(showA, showB, showC)

        // Across slots on same day (epochDay = 100): all 3 slots are different
        val morning = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, 100L)
        val afternoon = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.AFTERNOON, 100L)
        val night = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.NIGHT, 100L)

        // Note: Religion & Spirituality matches NIGHT_GENRES in our set, so night will match showA/showB/showC as matching
        // Let's check morning vs afternoon which have NO matching shows:
        assertTrue(morning?.id != afternoon?.id)
        assertTrue(afternoon?.id != night?.id)

        // Across consecutive days for the same slot (Morning):
        val morningDay0 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, 100L)
        val morningDay1 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, 101L)
        val morningDay2 = BecauseYouLikeRotationLogic.selectRotatedAnchor(candidates, BecauseYouLikeRotationLogic.DayPart.MORNING, 102L)

        assertTrue(morningDay0?.id != morningDay1?.id)
        assertTrue(morningDay1?.id != morningDay2?.id)
    }
}
