package cx.aswin.boxlore.feature.library.subscriptions

import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder

/**
 * Slot allocation result for a folder card in the Subscriptions grid.
 */
internal data class FolderSlots(
    val visibleShows: List<Podcast>,
    val overflowShows: List<Podcast>,
    val totalCount: Int,
) {
    val hasOverflow: Boolean get() = overflowShows.isNotEmpty()
    val overflowCount: Int get() = overflowShows.size
}

/**
 * Result of partitioning subscribed podcasts into folder members and unfiled shows.
 */
internal data class PartitionedSubscriptionItems(
    val pinnedFolders: List<SubscriptionFolder>,
    val compactFolders: List<SubscriptionFolder>,
    val unfiledPodcasts: List<Podcast>,
    val podcastsByFolderId: Map<String, List<Podcast>>,
)

/**
 * Data bundle for folder items in the subscriptions tab, keeping parameter lists concise.
 */
internal data class ShowsFolderItems(
    val pinnedFolders: List<SubscriptionFolder>,
    val compactFolders: List<SubscriptionFolder>,
    val podcastsByFolderId: Map<String, List<Podcast>>,
)

/**
 * Calculates the visible show covers and overflow shows for a given folder card display size.
 */
internal fun calculateFolderSlots(
    shows: List<Podcast>,
    displaySize: FolderDisplaySize,
): FolderSlots {
    val maxSlots = when (displaySize) {
        FolderDisplaySize.COMPACT -> 4
        FolderDisplaySize.WIDE -> 2
        FolderDisplaySize.FEATURED -> 4
        FolderDisplaySize.LARGE -> 6
        FolderDisplaySize.SHELF -> 3
        FolderDisplaySize.PANEL -> 6
        FolderDisplaySize.SHOWCASE -> 9
    }

    if (shows.size <= maxSlots) {
        return FolderSlots(
            visibleShows = shows,
            overflowShows = emptyList(),
            totalCount = shows.size,
        )
    }

    val visibleCount = maxSlots - 1
    return FolderSlots(
        visibleShows = shows.take(visibleCount),
        overflowShows = shows.drop(visibleCount),
        totalCount = shows.size,
    )
}

/**
 * Partitions subscribed podcasts into:
 * 1. Pinned enlarged folders (pinned to top of the grid).
 * 2. Compact 1×1 folders (can be placed in the grid).
 * 3. Unfiled podcasts (podcasts that do not belong to any active folder).
 */
internal fun partitionSubscribedShows(
    podcasts: List<Podcast>,
    folders: List<SubscriptionFolder>,
): PartitionedSubscriptionItems {
    val podcastsById = podcasts.associateBy { it.id }

    val podcastsByFolderId = folders.associate { folder ->
        folder.id to folder.podcastIds.mapNotNull(podcastsById::get)
    }

    val filedPodcastIds = folders.flatMap { it.podcastIds }.toSet()
    val unfiledPodcasts = podcasts.filter { it.id !in filedPodcastIds }

    val pinnedFolders = folders.filter { it.displaySize.isPinnedToTop }
    val compactFolders = folders.filter { !it.displaySize.isPinnedToTop }

    return PartitionedSubscriptionItems(
        pinnedFolders = pinnedFolders,
        compactFolders = compactFolders,
        unfiledPodcasts = unfiledPodcasts,
        podcastsByFolderId = podcastsByFolderId,
    )
}

/**
 * Filters folders by the selected genre chip.
 * If "All" is selected, all folders are returned.
 * Otherwise, returns folders whose name, linkedGenre, or member podcasts match the genre.
 */
internal fun filterFoldersByGenre(
    folders: List<SubscriptionFolder>,
    selectedGenre: String,
    podcasts: List<Podcast>,
): List<SubscriptionFolder> {
    if (selectedGenre.isBlank() || selectedGenre.equals("All", ignoreCase = true)) {
        return folders
    }

    val podcastsById = podcasts.associateBy { it.id }
    val resolved = resolveSubscriptionGenreItem(selectedGenre, podcasts)

    return folders.filter { folder ->
        folderMatchesGenre(
            folder = folder,
            selectedGenre = selectedGenre,
            resolvedLabel = resolved.label,
            resolvedValue = resolved.value,
            podcastsById = podcastsById,
        )
    }
}

private fun genreTokenMatches(
    candidate: String?,
    selectedGenre: String,
    resolvedLabel: String,
    resolvedValue: String,
): Boolean {
    if (candidate == null) return false
    val trimmed = candidate.trim()
    val isTechSynonym = (selectedGenre.equals("Tech", ignoreCase = true) && trimmed.equals("Technology", ignoreCase = true)) ||
        (selectedGenre.equals("Technology", ignoreCase = true) && trimmed.equals("Tech", ignoreCase = true))
    return trimmed.equals(selectedGenre, ignoreCase = true) ||
        trimmed.equals(resolvedValue, ignoreCase = true) ||
        trimmed.equals(resolvedLabel, ignoreCase = true) ||
        isTechSynonym
}

private fun folderMatchesGenre(
    folder: SubscriptionFolder,
    selectedGenre: String,
    resolvedLabel: String,
    resolvedValue: String,
    podcastsById: Map<String, Podcast>,
): Boolean {
    val nameMatches = genreTokenMatches(folder.name, selectedGenre, resolvedLabel, resolvedValue)
    val genreMatches = genreTokenMatches(folder.linkedGenre, selectedGenre, resolvedLabel, resolvedValue)
    if (nameMatches || genreMatches) return true

    return folder.podcastIds.any { podId ->
        val pod = podcastsById[podId] ?: return@any false
        pod.effectiveGenre.split(",")
            .map { it.trim() }
            .any { genreTokenMatches(it, selectedGenre, resolvedLabel, resolvedValue) } ||
            genreTokenMatches(pod.genre, selectedGenre, resolvedLabel, resolvedValue)
    }
}
