package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.database.ListeningHistoryEntity
import cx.aswin.boxlore.core.model.Episode
import cx.aswin.boxlore.core.model.EpisodeStatus
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.feature.library.ExpressiveSolarSystemEmptyState
import cx.aswin.boxlore.feature.library.LocalLastSeenEpisodes
import cx.aswin.boxlore.feature.library.PlayAllFab
import cx.aswin.boxlore.feature.library.SubscriptionSort
import cx.aswin.boxlore.feature.library.logic.SubscriptionManualOrderLogic
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val ShowsGenreHeaderKey = "shows_genre_header"
private val ShowsBlockedReorderKeys = setOf(ShowsGenreHeaderKey)

/** Callbacks for [ShowsTabContent], grouped to keep the composable parameter list small. */
internal data class ShowsTabActions(
    val onExploreClick: () -> Unit,
    val onPodcastClick: (String) -> Unit,
    val onReorder: (orderedIds: List<String>) -> Unit = {},
    val onNewFolderClick: (() -> Unit)? = null,
    val onFolderClick: (String) -> Unit = {},
    val onFolderLongClick: (SubscriptionFolder) -> Unit = {},
) {
    fun toFolderCardActions(): FolderCardActions = FolderCardActions(
        onPodcastClick = onPodcastClick,
        onFolderClick = onFolderClick,
        onFolderLongClick = onFolderLongClick,
    )
}

/** Configuration for [ShowsTabContent], grouped to keep the composable parameter list small. */
internal data class ShowsTabConfig(
    val isGridView: Boolean,
    val canReorder: Boolean = false,
    val pinnedPodcastIds: Set<String> = emptySet(),
    val sort: SubscriptionSort = SubscriptionSort.SmartRank,
    val manualOrder: List<String> = emptyList(),
)

private data class ShowsGridConfig(
    val reorderEnabled: Boolean,
    val pinnedPodcastIds: Set<String>,
)

@Composable
private fun rememberShowsOrderedKeys(
    partition: PartitionedSubscriptionItems,
    manualOrder: List<String>,
    isManualSort: Boolean,
): androidx.compose.runtime.MutableState<List<String>> {
    val incomingKeys = remember(partition, manualOrder, isManualSort) {
        buildUnifiedGridItems(
            compactFolders = partition.compactFolders,
            unfiledPodcasts = partition.unfiledPodcasts,
            podcastsByFolderId = partition.podcastsByFolderId,
            manualOrder = manualOrder,
            isManualSort = isManualSort,
        ).map { it.key }
    }
    val orderedState = remember { mutableStateOf(incomingKeys) }
    LaunchedEffect(incomingKeys) {
        orderedState.value = incomingKeys
    }
    return orderedState
}

@Composable
internal fun ShowsTabContent(
    podcasts: List<Podcast>,
    folders: List<SubscriptionFolder>,
    config: ShowsTabConfig,
    actions: ShowsTabActions,
) {
    if (podcasts.isEmpty()) {
        ExpressiveSolarSystemEmptyState(
            title = "No Subscriptions Yet",
            description = "Follow your favorite podcasts to see them here.",
            actionText = "Find Podcasts",
            onExploreClick = actions.onExploreClick,
        )
        return
    }
    val distinctGenres = remember(podcasts) { extractDistinctGenres(podcasts) }
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    val filteredPodcasts = remember(podcasts, selectedGenre) { filterPodcastsByGenre(podcasts, selectedGenre) }
    val distinctPodcasts = remember(filteredPodcasts) { filteredPodcasts.distinctBy { it.id } }
    val filteredFolders = remember(folders, selectedGenre, podcasts) {
        filterFoldersByGenre(folders, selectedGenre, podcasts)
    }
    val partition = remember(distinctPodcasts, filteredFolders, config.sort) {
        partitionSubscribedShows(distinctPodcasts, filteredFolders, config.sort)
    }
    val unfiledPodcasts = partition.unfiledPodcasts
    val reorderEnabled = config.canReorder && (selectedGenre.equals("All", ignoreCase = true) || selectedGenre.isBlank())
    val isManualSort = config.sort == SubscriptionSort.Manual

    var orderedKeys by rememberShowsOrderedKeys(partition, config.manualOrder, isManualSort)

    val unifiedGridItems = remember(partition, orderedKeys, isManualSort) {
        buildUnifiedGridItems(
            compactFolders = partition.compactFolders,
            unfiledPodcasts = partition.unfiledPodcasts,
            podcastsByFolderId = partition.podcastsByFolderId,
            manualOrder = orderedKeys,
            isManualSort = isManualSort,
        )
    }

    val blockedKeys = remember(partition.pinnedFolders) {
        ShowsBlockedReorderKeys + partition.pinnedFolders.map { "pinned_folder_${it.id}" }
    }

    val applyReorder: (String, String) -> Unit = { fromId, toId ->
        if (reorderEnabled) {
            val moved =
                SubscriptionManualOrderLogic.moveVisible(
                    ids = orderedKeys,
                    fromId = fromId,
                    toId = toId,
                    blockedKeys = blockedKeys,
                )
            if (moved != null) {
                orderedKeys = moved
                actions.onReorder(moved)
            }
        }
    }

    val genreChips: @Composable () -> Unit = {
        SubscriptionGenreChips(
            selectedGenre = selectedGenre,
            onGenreChange = {
                selectedGenre = it
                cx.aswin.boxlore.core.analytics.AnalyticsHelper.trackLibrarySubscriptionsGenreFiltered(it, "shows")
            },
            distinctGenres = distinctGenres,
            podcasts = podcasts,
            contentPadding = PaddingValues(horizontal = if (config.isGridView) 0.dp else 16.dp),
            onNewFolderClick = actions.onNewFolderClick,
        )
    }

    val folderItems = remember(partition) {
        ShowsFolderItems(
            pinnedFolders = partition.pinnedFolders,
            compactFolders = partition.compactFolders,
            podcastsByFolderId = partition.podcastsByFolderId,
        )
    }

    if (config.isGridView) {
        ShowsReorderableGrid(
            folderItems = folderItems,
            unifiedGridItems = unifiedGridItems,
            gridConfig = ShowsGridConfig(reorderEnabled, config.pinnedPodcastIds),
            actions = actions,
            onMove = applyReorder,
            genreChips = genreChips,
        )
    } else {
        val unfiledPodcastsById = remember(unfiledPodcasts) { unfiledPodcasts.associateBy { it.id } }
        val orderedPodcastsForList = remember(orderedKeys, unfiledPodcastsById, unfiledPodcasts) {
            if (isManualSort) {
                orderedKeys.mapNotNull(unfiledPodcastsById::get)
            } else {
                unfiledPodcasts
            }
        }
        ShowsReorderableList(
            orderedPodcasts = orderedPodcastsForList,
            folderItems = folderItems,
            gridConfig = ShowsGridConfig(reorderEnabled, config.pinnedPodcastIds),
            actions = actions,
            onMove = applyReorder,
            genreChips = genreChips,
        )
    }
}

@Composable
private fun ShowsReorderableGrid(
    folderItems: ShowsFolderItems,
    unifiedGridItems: List<SubscriptionGridItem>,
    gridConfig: ShowsGridConfig,
    actions: ShowsTabActions,
    onMove: (fromId: String, toId: String) -> Unit,
    genreChips: @Composable () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val reorderableGridState =
        rememberReorderableLazyGridState(gridState) { from, to ->
            val fromId = from.key as? String ?: return@rememberReorderableLazyGridState
            val toId = to.key as? String ?: return@rememberReorderableLazyGridState
            onMove(fromId, toId)
        }
    val folderActions = remember(actions) { actions.toFolderCardActions() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = ShowsGenreHeaderKey, span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                genreChips()
            }
        }

        // Pinned full-width enlarged folders (Shelf 3×1, Panel 3×2, Showcase 3×3, etc.)
        items(
            items = folderItems.pinnedFolders,
            key = { "pinned_folder_${it.id}" },
            span = { GridItemSpan(maxLineSpan) },
        ) { folder ->
            val folderShows = folderItems.podcastsByFolderId[folder.id].orEmpty()
            PinnedEnlargedFolderCard(
                folder = folder,
                podcasts = folderShows,
                actions = folderActions,
            )
        }

        // Unified 1×1 slots: Compact 1×1 folders and unfiled podcasts
        items(
            items = unifiedGridItems,
            key = { it.key },
            span = { GridItemSpan(1) },
        ) { item ->
            val lastSeenEpisodes = LocalLastSeenEpisodes.current
            ReorderableItem(
                reorderableGridState,
                key = item.key,
                enabled = gridConfig.reorderEnabled,
            ) { isDragging ->
                when (item) {
                    is SubscriptionGridItem.PodcastItem -> {
                        SubscriptionGridCard(
                            podcast = item.podcast,
                            lastSeenId = lastSeenEpisodes[item.podcast.id],
                            onClick = { actions.onPodcastClick(item.podcast.id) },
                            isPinned = item.podcast.id in gridConfig.pinnedPodcastIds,
                            isDragging = isDragging,
                            dragModifier =
                            if (gridConfig.reorderEnabled) {
                                Modifier.longPressDraggableHandle()
                            } else {
                                Modifier
                            },
                        )
                    }
                    is SubscriptionGridItem.FolderItem -> {
                        Compact1x1FolderCard(
                            folder = item.folder,
                            podcasts = item.podcasts,
                            actions = folderActions,
                            isDragging = isDragging,
                            dragModifier =
                            if (gridConfig.reorderEnabled) {
                                Modifier.longPressDraggableHandle()
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowsReorderableList(
    orderedPodcasts: List<Podcast>,
    folderItems: ShowsFolderItems,
    gridConfig: ShowsGridConfig,
    actions: ShowsTabActions,
    onMove: (fromId: String, toId: String) -> Unit,
    genreChips: @Composable () -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderableListState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromId = from.key as? String ?: return@rememberReorderableLazyListState
            val toId = to.key as? String ?: return@rememberReorderableLazyListState
            onMove(fromId, toId)
        }
    val folderActions = remember(actions) { actions.toFolderCardActions() }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 180.dp, top = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = ShowsGenreHeaderKey) {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                genreChips()
            }
        }
        items(
            items = folderItems.pinnedFolders + folderItems.compactFolders,
            key = { "list_folder_${it.id}" },
        ) { folder ->
            val folderShows = folderItems.podcastsByFolderId[folder.id].orEmpty()
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                PinnedEnlargedFolderCard(
                    folder = folder,
                    podcasts = folderShows,
                    actions = folderActions,
                )
            }
        }
        items(items = orderedPodcasts, key = { it.id }) { podcast ->
            ReorderableItem(
                reorderableListState,
                key = podcast.id,
                enabled = gridConfig.reorderEnabled,
            ) { isDragging ->
                SubscriptionListRow(
                    podcast = podcast,
                    onClick = { actions.onPodcastClick(podcast.id) },
                    isPinned = podcast.id in gridConfig.pinnedPodcastIds,
                    isDragging = isDragging,
                    dragModifier =
                    if (gridConfig.reorderEnabled) {
                        Modifier.longPressDraggableHandle()
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

/** Configuration for [LatestTabContent], grouped to keep the composable parameter list small. */
internal data class LatestTabConfig(
    val useSmartRank: Boolean,
    val hideCompleted: Boolean,
    val isPlayerActive: Boolean = false,
    val playAllBottomPadding: androidx.compose.ui.unit.Dp? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LatestTabContent(
    podcasts: List<Podcast>,
    allHistory: List<ListeningHistoryEntity>,
    config: LatestTabConfig,
    scoreEpisodes: suspend (
        List<Podcast>,
        List<ListeningHistoryEntity>,
    ) -> Map<String, Double>,
    actions: LatestTabActions,
) {
    val allWithLatest = remember(podcasts) { podcasts.filter { it.latestEpisode != null } }
    val episodePodcasts = remember(allWithLatest, config.hideCompleted) {
        if (config.hideCompleted) {
            allWithLatest.filter { it.episodeStatus != EpisodeStatus.COMPLETED }
        } else {
            allWithLatest
        }
    }

    when {
        allWithLatest.isEmpty() ->
            ExpressiveSolarSystemEmptyState(
                title = "No New Episodes",
                description = "You're all caught up! Explore for more content.",
                actionText = "Discover Shows",
                onExploreClick = actions.onExploreClick,
            )
        episodePodcasts.isEmpty() ->
            ExpressiveSolarSystemEmptyState(
                title = "You're all caught up",
                description = "Hidden played episodes are out of the way. Turn off Hide played in Sort to see them again.",
                actionText = "Discover Shows",
                onExploreClick = actions.onExploreClick,
            )
        else ->
            LatestEpisodesList(
                episodePodcasts = episodePodcasts,
                allHistory = allHistory,
                config = config,
                scoreEpisodes = scoreEpisodes,
                actions = actions,
            )
    }
}

/** Callbacks for [LatestTabContent], grouped to keep the composable parameter list small. */
internal data class LatestTabActions(
    val onExploreClick: () -> Unit,
    val onEpisodeClick: ((Episode, Podcast, String?) -> Unit)?,
    val onPlayEpisode: ((Episode, Podcast) -> Unit)?,
    val onPlayEpisodes: ((List<Episode>, Podcast) -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LatestEpisodesList(
    episodePodcasts: List<Podcast>,
    allHistory: List<ListeningHistoryEntity>,
    config: LatestTabConfig,
    scoreEpisodes: suspend (
        List<Podcast>,
        List<ListeningHistoryEntity>,
    ) -> Map<String, Double>,
    actions: LatestTabActions,
) {
    val distinctGenres = remember(episodePodcasts) { extractDistinctGenres(episodePodcasts) }
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    val filteredEpisodePodcasts = remember(episodePodcasts, selectedGenre) {
        filterPodcastsByGenre(episodePodcasts, selectedGenre)
    }
    val episodeScores by produceState(
        initialValue = emptyMap<String, Double>(),
        filteredEpisodePodcasts,
        allHistory,
        config.useSmartRank,
    ) {
        value = scoreLatestIfNeeded(config.useSmartRank, filteredEpisodePodcasts, allHistory, scoreEpisodes)
    }
    val displayPodcasts = remember(filteredEpisodePodcasts, config.useSmartRank, episodeScores) {
        sortLatestDisplayPodcasts(filteredEpisodePodcasts, config.useSmartRank, episodeScores)
    }
    val groupedEpisodes = remember(displayPodcasts, config.useSmartRank) {
        groupLatestByDateHeader(displayPodcasts, config.useSmartRank)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 240.dp, top = 4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                SubscriptionGenreChips(
                    selectedGenre = selectedGenre,
                    onGenreChange = {
                        selectedGenre = it
                        cx.aswin.boxlore.core.analytics.AnalyticsHelper
                            .trackLibrarySubscriptionsGenreFiltered(it, "latest")
                    },
                    distinctGenres = distinctGenres,
                    podcasts = episodePodcasts,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            latestEpisodeItems(
                useSmartRank = config.useSmartRank,
                displayPodcasts = displayPodcasts,
                groupedEpisodes = groupedEpisodes,
                actions = actions,
            )
        }
        LatestPlayAllFab(
            displayPodcasts = displayPodcasts,
            isPlayerActive = config.isPlayerActive,
            onPlayEpisodes = actions.onPlayEpisodes,
            bottomPaddingOverride = config.playAllBottomPadding,
        )
    }
}

internal suspend fun scoreLatestIfNeeded(
    useSmartRank: Boolean,
    podcasts: List<Podcast>,
    history: List<ListeningHistoryEntity>,
    scoreEpisodes: suspend (List<Podcast>, List<ListeningHistoryEntity>) -> Map<String, Double>,
): Map<String, Double> = if (useSmartRank) scoreEpisodes(podcasts, history) else emptyMap()

internal fun sortLatestDisplayPodcasts(
    podcasts: List<Podcast>,
    useSmartRank: Boolean,
    episodeScores: Map<String, Double>,
): List<Podcast> = if (useSmartRank) {
    podcasts.sortedByDescending { episodeScores[it.latestEpisode?.id] ?: 0.0 }
} else {
    podcasts.sortedByDescending { it.latestEpisode!!.publishedDate }
}

internal fun groupLatestByDateHeader(
    podcasts: List<Podcast>,
    useSmartRank: Boolean,
): Map<String, List<Podcast>> = if (useSmartRank) {
    emptyMap()
} else {
    podcasts.groupBy { getChronologicalHeader(it.latestEpisode!!.publishedDate) }
}

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.latestEpisodeItems(
    useSmartRank: Boolean,
    displayPodcasts: List<Podcast>,
    groupedEpisodes: Map<String, List<Podcast>>,
    actions: LatestTabActions,
) {
    if (useSmartRank) {
        items(items = displayPodcasts, key = { "${it.id}_latest_smart" }) { podcast ->
            LatestEpisodeListRow(podcast = podcast, actions = actions)
        }
        return
    }
    groupedEpisodes.forEach { (header, podcastsInGroup) ->
        stickyHeader { DateHeader(text = header) }
        items(items = podcastsInGroup, key = { "${it.id}_latest_chrono" }) { podcast ->
            LatestEpisodeListRow(podcast = podcast, actions = actions)
        }
    }
}

@Composable
private fun BoxScope.LatestPlayAllFab(
    displayPodcasts: List<Podcast>,
    isPlayerActive: Boolean,
    onPlayEpisodes: ((List<Episode>, Podcast) -> Unit)?,
    bottomPaddingOverride: androidx.compose.ui.unit.Dp? = null,
) {
    if (displayPodcasts.isEmpty() || onPlayEpisodes == null) return
    val firstPodcast = displayPodcasts.firstOrNull() ?: return
    PlayAllFab(
        isPlayerActive = isPlayerActive,
        bottomPaddingOverride = bottomPaddingOverride,
        onClick = {
            onPlayEpisodes(displayPodcasts.map { it.latestEpisode!! }, firstPodcast)
        },
    )
}

@Composable
private fun LatestEpisodeListRow(
    podcast: Podcast,
    actions: LatestTabActions,
) {
    val episode = podcast.latestEpisode!!
    LatestEpisodeRow(
        episode = episode,
        podcast = podcast,
        onClick = { actions.onEpisodeClick?.invoke(episode, podcast, "library_latest_episodes") },
        onPlay =
        if (actions.onPlayEpisode != null) {
            { actions.onPlayEpisode.invoke(episode, podcast) }
        } else {
            null
        },
    )
}
