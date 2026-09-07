package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * State representing which section is currently in reordering mode.
 */
sealed interface ReorderMode {
    data object Inactive : ReorderMode
    data object Folders : ReorderMode
    data class FolderShows(val folderId: String) : ReorderMode
    data object RootShows : ReorderMode
}

/**
 * Floating bar displayed during active [ReorderMode], providing a notice that sort will switch
 * to manual, along with floating Save and Cancel action buttons.
 */
@Composable
internal fun SubscriptionReorderBar(
    reorderMode: ReorderMode,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = reorderMode != ReorderMode.Inactive,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        val message = when (reorderMode) {
            ReorderMode.Folders -> "Your folder sort will be switched to manual"
            is ReorderMode.FolderShows -> "Your sort inside this folder will be switched to manual"
            ReorderMode.RootShows -> "Your subscription sort will be switched to manual"
            ReorderMode.Inactive -> ""
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Reorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Reorder Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cancel Button
                    IconButton(
                        onClick = onCancel,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel reordering",
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Save Button
                    FilledIconButton(
                        onClick = onSave,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Save new order",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

internal const val ShowsGenreHeaderKey = "shows_genre_header"
internal val ShowsBlockedReorderKeys = setOf(ShowsGenreHeaderKey)

internal fun isFolderKey(key: String): Boolean =
    key.startsWith("compact_folder_") ||
        key.startsWith("pinned_folder_") ||
        key.startsWith("list_folder_")

internal fun extractFolderId(key: String): String? {
    val id = key.removePrefix("compact_folder_")
        .removePrefix("pinned_folder_")
        .removePrefix("list_folder_")
    return if (id != key) id else null
}

internal fun isReorderablePodcastKey(key: String): Boolean =
    !isFolderKey(key) && key != ShowsGenreHeaderKey

@Composable
internal fun rememberShowsOrderedKeys(
    unfiledPodcasts: List<cx.aswin.boxlore.core.model.Podcast>,
    manualOrder: List<String>,
    isManualSort: Boolean,
    isRootReordering: Boolean = false,
): androidx.compose.runtime.MutableState<List<String>> {
    val incomingKeys = androidx.compose.runtime.remember(unfiledPodcasts, manualOrder, isManualSort, isRootReordering) {
        if ((isManualSort || isRootReordering) && manualOrder.isNotEmpty()) {
            val podIds = unfiledPodcasts.map { it.id }.toSet()
            val ordered = manualOrder.filter { it in podIds }
            val remainder = unfiledPodcasts.map { it.id }.filter { it !in manualOrder }
            ordered + remainder
        } else {
            unfiledPodcasts.map { it.id }
        }
    }
    val orderedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(incomingKeys) }
    androidx.compose.runtime.LaunchedEffect(incomingKeys) {
        orderedState.value = incomingKeys
    }
    return orderedState
}

internal fun resolveOrderedPodcasts(
    unfiledPodcasts: List<cx.aswin.boxlore.core.model.Podcast>,
    orderedKeys: List<String>,
    isManualSort: Boolean,
    isRootReordering: Boolean = false,
): List<cx.aswin.boxlore.core.model.Podcast> {
    if (!isManualSort && !isRootReordering) return unfiledPodcasts
    val unfiledPodcastsById = unfiledPodcasts.associateBy { it.id }
    val mapped = orderedKeys.mapNotNull(unfiledPodcastsById::get)
    val missing = unfiledPodcasts.filter { it.id !in orderedKeys }
    return mapped + missing
}

@Composable
internal fun rememberShowsMoveHandler(
    isFoldersReordering: Boolean,
    isRootReordering: Boolean,
    orderedFolderKeys: List<String>,
    onFolderKeysChange: (List<String>) -> Unit,
    orderedKeys: List<String>,
    onKeysChange: (List<String>) -> Unit,
    actions: ShowsTabActions,
): (String, String) -> Unit = androidx.compose.runtime.remember(
    isFoldersReordering,
    isRootReordering,
    orderedFolderKeys,
    orderedKeys,
    actions,
) {
    { fromKey, toKey ->
        if (isFoldersReordering) {
            val fromFolderId = extractFolderId(fromKey)
            val toFolderId = extractFolderId(toKey)
            if (fromFolderId != null && toFolderId != null) {
                val moved = cx.aswin.boxlore.feature.library.logic.SubscriptionManualOrderLogic.move(
                    orderedFolderKeys,
                    fromFolderId,
                    toFolderId,
                )
                onFolderKeysChange(moved)
                actions.onReorderFolders(moved)
            }
        } else if (isRootReordering && isReorderablePodcastKey(fromKey) && isReorderablePodcastKey(toKey)) {
            val moved = cx.aswin.boxlore.feature.library.logic.SubscriptionManualOrderLogic.moveVisible(
                ids = orderedKeys,
                fromId = fromKey,
                toId = toKey,
                blockedKeys = ShowsBlockedReorderKeys,
            )
            if (moved != null) {
                onKeysChange(moved)
                actions.onReorder(moved)
            }
        }
    }
}

@Composable
internal fun rememberShowsFolderItems(
    partition: PartitionedSubscriptionItems,
    orderedFolderKeys: List<String>,
    isFoldersReordering: Boolean,
    folderSort: FolderInterSort,
): ShowsFolderItems = androidx.compose.runtime.remember(
    partition,
    orderedFolderKeys,
    isFoldersReordering,
    folderSort,
) {
    if (isFoldersReordering || folderSort == FolderInterSort.Manual) {
        val pinnedMap = partition.pinnedFolders.associateBy { it.id }
        val compactMap = partition.compactFolders.associateBy { it.id }
        val reorderedPinned = orderedFolderKeys.mapNotNull { pinnedMap[it] }
        val reorderedCompact = orderedFolderKeys.mapNotNull { compactMap[it] }
        val missingPinned = partition.pinnedFolders.filter { it.id !in orderedFolderKeys }
        val missingCompact = partition.compactFolders.filter { it.id !in orderedFolderKeys }
        ShowsFolderItems(
            pinnedFolders = reorderedPinned + missingPinned,
            compactFolders = reorderedCompact + missingCompact,
            podcastsByFolderId = partition.podcastsByFolderId,
        )
    } else {
        ShowsFolderItems(
            pinnedFolders = partition.pinnedFolders,
            compactFolders = partition.compactFolders,
            podcastsByFolderId = partition.podcastsByFolderId,
        )
    }
}
