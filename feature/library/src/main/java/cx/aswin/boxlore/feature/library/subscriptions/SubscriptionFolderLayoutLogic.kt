package cx.aswin.boxlore.feature.library.subscriptions

import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.core.model.isLatestEpisodeNew
import cx.aswin.boxlore.feature.library.SubscriptionSort

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
 * Result of partitioning subscribed podcasts into:
 * 1. Pinned enlarged folders (pinned to top of the grid).
 * 2. Compact 1×1 folders (can be placed in the grid).
 * 3. Unfiled podcasts (podcasts that do not belong to any active folder).
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
 * Unified item representation for the 1×1 slots in the Subscriptions grid,
 * allowing 1×1 compact folders and unfiled podcasts to share the same drag-reorderable sequence.
 */
internal sealed class SubscriptionGridItem {
    abstract val key: String

    data class PodcastItem(
        val podcast: Podcast,
    ) : SubscriptionGridItem() {
        override val key: String get() = podcast.id
    }

    data class FolderItem(
        val folder: SubscriptionFolder,
        val podcasts: List<Podcast>,
    ) : SubscriptionGridItem() {
        override val key: String get() = "folder:${folder.id}"
    }
}

/**
 * Builds the unified sequence of 1×1 grid items (compact folders and unfiled podcasts).
 * When in Manual sort mode, items follow [manualOrder] with unplaced items appended.
 */
internal fun buildUnifiedGridItems(
    compactFolders: List<SubscriptionFolder>,
    unfiledPodcasts: List<Podcast>,
    podcastsByFolderId: Map<String, List<Podcast>>,
    manualOrder: List<String> = emptyList(),
    isManualSort: Boolean = false,
): List<SubscriptionGridItem> {
    val podcastItems = unfiledPodcasts.map { SubscriptionGridItem.PodcastItem(it) }
    val folderItems = compactFolders.map { folder ->
        SubscriptionGridItem.FolderItem(folder, podcastsByFolderId[folder.id].orEmpty())
    }

    val allItems = folderItems + podcastItems
    if (!isManualSort || manualOrder.isEmpty()) {
        return allItems
    }

    val itemsByKey = allItems.associateBy { it.key }
    val seen = LinkedHashSet<String>()
    val result = ArrayList<SubscriptionGridItem>(allItems.size)

    for (key in manualOrder) {
        val item = itemsByKey[key] ?: continue
        if (seen.add(key)) {
            result.add(item)
        }
    }

    allItems.filter { it.key !in seen }.forEach { result.add(it) }
    return result
}

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
 * Partitions subscribed podcasts into pinned enlarged folders, compact 1×1 folders, and unfiled shows.
 * In-folder shows are automatically sorted to match the active list order of [podcasts].
 * Pinned folders are sorted according to [sort] (e.g. freshest episode date for RecentlyUpdated, A-Z for Alphabetical).
 */
internal fun partitionSubscribedShows(
    podcasts: List<Podcast>,
    folders: List<SubscriptionFolder>,
    sort: SubscriptionSort? = null,
): PartitionedSubscriptionItems {
    val podcastsById = podcasts.associateBy { it.id }

    val podcastsByFolderId = folders.associate { folder ->
        val memberIds = folder.podcastIds.toSet()
        val sortedMembers = podcasts.filter { it.id in memberIds }
        val missingMembers = folder.podcastIds.filter { it !in memberIds }.mapNotNull(podcastsById::get)
        folder.id to (sortedMembers + missingMembers)
    }

    val filedPodcastIds = folders.flatMap { it.podcastIds }.toSet()
    val unfiledPodcasts = podcasts.filter { it.id !in filedPodcastIds }

    val pinnedFoldersRaw = folders.filter { it.displaySize.isPinnedToTop }
    val compactFoldersRaw = folders.filter { !it.displaySize.isPinnedToTop }

    val sortFolder: (SubscriptionFolder) -> Long = { folder ->
        podcastsByFolderId[folder.id].orEmpty().maxOfOrNull { it.latestEpisode?.publishedDate ?: 0L } ?: 0L
    }

    val pinnedFolders = when (sort) {
        SubscriptionSort.Alphabetical -> pinnedFoldersRaw.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SubscriptionSort.RecentlyUpdated -> pinnedFoldersRaw.sortedByDescending(sortFolder)
        else -> pinnedFoldersRaw
    }

    val compactFolders = when (sort) {
        SubscriptionSort.Alphabetical -> compactFoldersRaw.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SubscriptionSort.RecentlyUpdated -> compactFoldersRaw.sortedByDescending(sortFolder)
        else -> compactFoldersRaw
    }

    return PartitionedSubscriptionItems(
        pinnedFolders = pinnedFolders,
        compactFolders = compactFolders,
        unfiledPodcasts = unfiledPodcasts,
        podcastsByFolderId = podcastsByFolderId,
    )
}

/**
 * Returns the count of shows within the overflow cluster that have a new episode.
 */
internal fun countFolderOverflowNew(
    overflowShows: List<Podcast>,
    lastSeenEpisodes: Map<String, String>,
): Int = overflowShows.count { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) }

/**
 * Returns true if any show within the overflow cluster has a new episode.
 */
internal fun hasFolderOverflowNew(
    overflowShows: List<Podcast>,
    lastSeenEpisodes: Map<String, String>,
): Boolean = countFolderOverflowNew(overflowShows, lastSeenEpisodes) > 0

/**
 * Returns true if any show within the folder has a new episode.
 */
internal fun hasAnyFolderShowNew(
    podcasts: List<Podcast>,
    lastSeenEpisodes: Map<String, String>,
): Boolean = podcasts.any { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) }

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
