package cx.aswin.boxlore.core.downloads

/**
 * The user-facing intent when triggering a download action on an episode.
 */
enum class DownloadToggleAction {
    /**
     * The episode is already fully downloaded. Removing it requires user confirmation
     * to prevent accidental deletion of local media files.
     */
    CONFIRM_REMOVAL,

    /**
     * An in-progress download is actively fetching. Cancelling it immediately halts
     * network operations without confirmation.
     */
    CANCEL_DOWNLOAD,

    /**
     * The episode is neither downloaded nor actively downloading. Initiating a download
     * starts immediately without confirmation.
     */
    START_DOWNLOAD,
}

/**
 * Pure policy determining the appropriate action when toggling download state for an episode.
 */
object DownloadTogglePolicy {
    fun resolveAction(isDownloaded: Boolean, isDownloading: Boolean): DownloadToggleAction =
        when {
            isDownloaded -> DownloadToggleAction.CONFIRM_REMOVAL
            isDownloading -> DownloadToggleAction.CANCEL_DOWNLOAD
            else -> DownloadToggleAction.START_DOWNLOAD
        }
}
