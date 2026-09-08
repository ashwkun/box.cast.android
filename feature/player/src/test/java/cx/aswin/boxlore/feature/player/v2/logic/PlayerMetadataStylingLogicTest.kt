package cx.aswin.boxlore.feature.player.v2.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerMetadataStylingLogicTest {

    private val onSurface = Color(0xFFE2E2E6)
    private val onSurfaceVariant = Color(0xFFC4C6D0)

    @Test
    fun transcriptModeMutedColorsDoNotConflictWithInactiveTranscript() {
        val normalColors = PlayerMetadataStylingLogic.resolveTargetColors(
            isTranscriptMode = false,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
        )
        assertEquals(onSurface, normalColors.episodeColor)
        assertEquals(onSurfaceVariant, normalColors.podcastColor)

        val transcriptColors = PlayerMetadataStylingLogic.resolveTargetColors(
            isTranscriptMode = true,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
        )

        // Must be muted compared to active/normal elements
        assertTrue(transcriptColors.episodeColor.alpha < 1.0f)
        assertTrue(transcriptColors.podcastColor.alpha < 1.0f)

        // Must NOT conflict with inactive transcript lines (0.35f)
        val inactiveTranscriptAlpha = PlayerMetadataStylingLogic.INACTIVE_TRANSCRIPT_ALPHA
        assertTrue(transcriptColors.episodeColor.alpha > inactiveTranscriptAlpha)
        assertTrue(transcriptColors.podcastColor.alpha > inactiveTranscriptAlpha)

        // Hierarchy between episode and podcast title
        assertTrue(transcriptColors.episodeColor.alpha > transcriptColors.podcastColor.alpha)
        assertEquals(PlayerMetadataStylingLogic.TRANSCRIPT_MODE_EPISODE_ALPHA, transcriptColors.episodeColor.alpha, 0.01f)
        assertEquals(PlayerMetadataStylingLogic.TRANSCRIPT_MODE_PODCAST_ALPHA, transcriptColors.podcastColor.alpha, 0.01f)
    }

    @Test
    fun metadataSpacingIsCompactedInTranscriptMode() {
        val (transcriptTop, transcriptBottom) = PlayerMetadataStylingLogic.resolveMetadataSpacing(
            isTranscriptMode = true,
            isCompact = false,
        )
        assertEquals(6.dp, transcriptTop)
        assertEquals(6.dp, transcriptBottom)

        val (normalTop, normalBottom) = PlayerMetadataStylingLogic.resolveMetadataSpacing(
            isTranscriptMode = false,
            isCompact = false,
        )
        assertEquals(16.dp, normalTop)
        assertEquals(14.dp, normalBottom)

        val (compactTop, compactBottom) = PlayerMetadataStylingLogic.resolveMetadataSpacing(
            isTranscriptMode = false,
            isCompact = true,
        )
        assertEquals(10.dp, compactTop)
        assertEquals(8.dp, compactBottom)
    }

    @Test
    fun marqueeVelocityIsCalibratedForTranscriptMode() {
        val (transcriptEpVelocity, transcriptPodVelocity) =
            PlayerMetadataStylingLogic.resolveMarqueeVelocity(isTranscriptMode = true)
        assertEquals(20.dp, transcriptEpVelocity)
        assertEquals(18.dp, transcriptPodVelocity)

        val (normalEpVelocity, normalPodVelocity) =
            PlayerMetadataStylingLogic.resolveMarqueeVelocity(isTranscriptMode = false)
        assertEquals(26.dp, normalEpVelocity)
        assertEquals(24.dp, normalPodVelocity)
    }
}
