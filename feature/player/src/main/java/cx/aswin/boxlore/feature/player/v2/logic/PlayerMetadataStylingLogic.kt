package cx.aswin.boxlore.feature.player.v2.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class PlayerMetadataColors(
    val episodeColor: Color,
    val podcastColor: Color,
)

/**
 * Pure styling logic for player metadata (episode name and podcast title) across hero modes.
 */
internal object PlayerMetadataStylingLogic {

    const val INACTIVE_TRANSCRIPT_ALPHA = 0.35f
    const val TRANSCRIPT_MODE_EPISODE_ALPHA = 0.72f
    const val TRANSCRIPT_MODE_PODCAST_ALPHA = 0.54f

    fun resolveMetadataSpacing(isTranscriptMode: Boolean, isCompact: Boolean): Pair<Dp, Dp> {
        val top = when {
            isTranscriptMode -> 6.dp
            isCompact -> 10.dp
            else -> 16.dp
        }
        val bottom = when {
            isTranscriptMode -> 6.dp
            isCompact -> 8.dp
            else -> 14.dp
        }
        return top to bottom
    }

    fun resolveMarqueeVelocity(isTranscriptMode: Boolean): Pair<Dp, Dp> {
        val episodeVelocity = if (isTranscriptMode) 20.dp else 26.dp
        val podcastVelocity = if (isTranscriptMode) 18.dp else 24.dp
        return episodeVelocity to podcastVelocity
    }

    fun resolveTargetColors(
        isTranscriptMode: Boolean,
        onSurface: Color,
        onSurfaceVariant: Color,
    ): PlayerMetadataColors = if (isTranscriptMode) {
        PlayerMetadataColors(
            episodeColor = onSurface.copy(alpha = TRANSCRIPT_MODE_EPISODE_ALPHA),
            podcastColor = onSurface.copy(alpha = TRANSCRIPT_MODE_PODCAST_ALPHA),
        )
    } else {
        PlayerMetadataColors(
            episodeColor = onSurface,
            podcastColor = onSurfaceVariant,
        )
    }
}
