package cx.aswin.boxlore.core.downloads

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadTogglePolicyTest {
    @Test
    fun downloadedEpisodeRequiresRemovalConfirmation() {
        assertEquals(
            DownloadToggleAction.CONFIRM_REMOVAL,
            DownloadTogglePolicy.resolveAction(isDownloaded = true, isDownloading = false),
        )
        // If somehow both flags are set, removal confirmation still takes precedence for safety
        assertEquals(
            DownloadToggleAction.CONFIRM_REMOVAL,
            DownloadTogglePolicy.resolveAction(isDownloaded = true, isDownloading = true),
        )
    }

    @Test
    fun activeDownloadCancelsImmediatelyWithoutConfirmation() {
        assertEquals(
            DownloadToggleAction.CANCEL_DOWNLOAD,
            DownloadTogglePolicy.resolveAction(isDownloaded = false, isDownloading = true),
        )
    }

    @Test
    fun unDownloadedEpisodeStartsDownloadImmediately() {
        assertEquals(
            DownloadToggleAction.START_DOWNLOAD,
            DownloadTogglePolicy.resolveAction(isDownloaded = false, isDownloading = false),
        )
    }
}
