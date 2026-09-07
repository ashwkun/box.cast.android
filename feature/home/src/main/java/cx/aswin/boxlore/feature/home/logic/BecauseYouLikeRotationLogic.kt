package cx.aswin.boxlore.feature.home.logic

import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.PodcastGenres
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Time-of-day rotation and cross-day variation logic for the Home screen's
 * "Because You Like <Show>" recommendation rail (Issue #1049).
 */
object BecauseYouLikeRotationLogic {

    enum class DayPart {
        MORNING, // 05:00 - 11:59
        AFTERNOON, // 12:00 - 19:59
        NIGHT, // 20:00 - 04:59
    }

    val MORNING_GENRES: Set<String> = setOf(
        PodcastGenres.NEWS,
        PodcastGenres.TECHNOLOGY,
        PodcastGenres.BUSINESS,
        PodcastGenres.EDUCATION,
        PodcastGenres.GOVERNMENT,
    )

    val AFTERNOON_GENRES: Set<String> = setOf(
        PodcastGenres.COMEDY,
        PodcastGenres.SOCIETY_AND_CULTURE,
        PodcastGenres.SPORTS,
        PodcastGenres.ARTS,
        PodcastGenres.TV_AND_FILM,
        PodcastGenres.MUSIC,
        PodcastGenres.LEISURE,
    )

    val NIGHT_GENRES: Set<String> = setOf(
        PodcastGenres.TRUE_CRIME,
        PodcastGenres.HISTORY,
        PodcastGenres.SCIENCE,
        PodcastGenres.FICTION,
        PodcastGenres.RELIGION_AND_SPIRITUALITY,
        PodcastGenres.HEALTH,
    )

    /**
     * Resolves the [DayPart] from the hour of the day (0..23).
     * - Morning: 05:00 - 11:59
     * - Afternoon/Evening: 12:00 - 19:59
     * - Night: 20:00 - 04:59
     */
    fun resolveDayPart(hourOfDay: Int): DayPart = when (hourOfDay) {
        in 5..11 -> DayPart.MORNING
        in 12..19 -> DayPart.AFTERNOON
        else -> DayPart.NIGHT
    }

    /**
     * Computes a stable slot key combining the epoch day and day part name.
     * e.g. "20674:MORNING".
     */
    fun computeSlotKey(epochDay: Long, dayPart: DayPart): String = "$epochDay:${dayPart.name}"

    /**
     * Returns the current slot key using the specified [Clock].
     */
    fun currentSlotKey(clock: Clock = Clock.systemDefaultZone()): String {
        val now = ZonedDateTime.now(clock)
        val dayPart = resolveDayPart(now.hour)
        return computeSlotKey(now.toLocalDate().toEpochDay(), dayPart)
    }

    /**
     * Checks if a podcast matches the preferred genres for a given [DayPart].
     */
    fun matchesDayPart(podcast: Podcast, dayPart: DayPart): Boolean =
        matchesCanonicalGenres(podcast, dayPart) || matchesRawKeywords(podcast, dayPart)

    private fun matchesCanonicalGenres(podcast: Podcast, dayPart: DayPart): Boolean {
        val targetGenres = targetGenresFor(dayPart)
        val candidates = listOfNotNull(
            PodcastGenres.canonicalize(podcast.customGenre),
            PodcastGenres.canonicalize(podcast.genre),
            PodcastGenres.canonicalize(podcast.effectiveGenre),
            PodcastGenres.canonicalize(podcast.recommendationGenre),
        )
        return candidates.any { targetGenres.contains(it) }
    }

    private fun matchesRawKeywords(podcast: Podcast, dayPart: DayPart): Boolean {
        val rawText = "${podcast.effectiveGenre} ${podcast.description.orEmpty()}".lowercase()
        val keywords = when (dayPart) {
            DayPart.MORNING -> listOf("news", "tech", "business", "daily")
            DayPart.AFTERNOON -> listOf("comedy", "culture", "sport", "entertainment")
            DayPart.NIGHT -> listOf("crime", "history", "mystery", "science", "sleep")
        }
        return keywords.any { rawText.contains(it) }
    }

    private fun targetGenresFor(dayPart: DayPart): Set<String> = when (dayPart) {
        DayPart.MORNING -> MORNING_GENRES
        DayPart.AFTERNOON -> AFTERNOON_GENRES
        DayPart.NIGHT -> NIGHT_GENRES
    }

    /**
     * Filters candidate podcast IDs from affinity scores based on engagement threshold.
     * Podcasts with score >= maxOf(minScore, topScore * thresholdRatio) are considered eligible.
     * Candidates are ordered by score descending, then by lastPlayedAt descending, capped at [maxCandidates].
     */
    fun filterEligibleCandidates(
        scores: Map<String, Int>,
        lastPlayedMap: Map<String, Long>,
        minScore: Int = 15,
        thresholdRatio: Double = 0.5,
        maxCandidates: Int = 10,
    ): List<String> {
        if (scores.isEmpty()) return emptyList()
        val topScore = scores.values.maxOrNull() ?: 0
        if (topScore < minScore) return emptyList()

        val threshold = maxOf(minScore, (topScore * thresholdRatio).toInt())
        return scores.entries
            .filter { it.value >= threshold }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { lastPlayedMap.getOrDefault(it.key, 0L) },
            )
            .take(maxCandidates)
            .map { it.key }
    }

    /**
     * Selects a rotated anchor podcast from the eligible [candidates] pool.
     *
     * Ensures cross-day variation so the same show is not repeatedly shown at the same
     * time slot day after day, adapting to pool strength:
     * - If 2+ candidates match the day-part genre: rotates through the matching shows across days.
     * - If exactly 1 candidate matches: alternates between the matching show (even days) and other
     *   eligible shows from the pool (odd days).
     * - If no candidates match: rotates through all eligible candidates using both epochDay and dayPart.
     */
    fun selectRotatedAnchor(
        candidates: List<Podcast>,
        dayPart: DayPart,
        epochDay: Long,
    ): Podcast? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val matching = candidates.filter { matchesDayPart(it, dayPart) }

        return when {
            matching.size >= 2 -> {
                val index = Math.floorMod(epochDay, matching.size.toLong()).toInt()
                matching[index]
            }
            matching.size == 1 -> {
                val match = matching.first()
                val others = candidates.filter { it.id != match.id }
                if (others.isEmpty() || Math.floorMod(epochDay, 2L) == 0L) {
                    match
                } else {
                    val index = Math.floorMod(epochDay / 2L + dayPart.ordinal, others.size.toLong()).toInt()
                    others[index]
                }
            }
            else -> {
                val index = Math.floorMod(epochDay + dayPart.ordinal, candidates.size.toLong()).toInt()
                candidates[index]
            }
        }
    }
}
