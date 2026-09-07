package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.feature.library.SubscriptionSort

/**
 * State bundle for [SubscriptionSortSheet].
 */
internal data class SubscriptionSortConfig(
    val currentSort: SubscriptionSort,
    val folderSort: FolderInterSort,
    val intraFolderSort: FolderIntraSort,
    val autoOrganizeFolders: Boolean,
)

/**
 * Actions bundle for [SubscriptionSortSheet].
 */
internal data class SubscriptionSortActions(
    val onSortChange: (SubscriptionSort) -> Unit,
    val onFolderSortChange: (FolderInterSort) -> Unit,
    val onIntraFolderSortChange: (FolderIntraSort) -> Unit,
    val onAutoOrganizeFoldersChange: (Boolean) -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Bottom sheet providing granular multi-tier sorting controls for subscriptions:
 * 1. Shows outside folders (individual library podcasts).
 * 2. Folders at the top of the library.
 * 3. Shows inside folders.
 * Also includes an informative tip regarding press-and-hold repositioning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionSortSheet(
    config: SubscriptionSortConfig,
    actions: SubscriptionSortActions,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            SheetHeader(onDismiss = actions.onDismiss)

            Spacer(modifier = Modifier.height(12.dp))

            DragRepositionTipBanner()

            Spacer(modifier = Modifier.height(16.dp))

            ShowsSortSection(
                currentSort = config.currentSort,
                onSortChange = actions.onSortChange,
            )

            Spacer(modifier = Modifier.height(18.dp))

            FolderSortSection(
                folderSort = config.folderSort,
                onFolderSortChange = actions.onFolderSortChange,
            )

            Spacer(modifier = Modifier.height(18.dp))

            IntraFolderSortSection(
                intraFolderSort = config.intraFolderSort,
                onIntraFolderSortChange = actions.onIntraFolderSortChange,
            )

            Spacer(modifier = Modifier.height(18.dp))

            ActiveSortSummaryCard(
                currentSort = config.currentSort,
                folderSort = config.folderSort,
                intraFolderSort = config.intraFolderSort,
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AutoOrganizeRow(
                autoOrganize = config.autoOrganizeFolders,
                onToggle = actions.onAutoOrganizeFoldersChange,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = "Subscription Sorting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = GoogleSansWeight.bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Organize your shows & folders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DragRepositionTipBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Press & hold to reposition",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You can drag and reorder shows outside folders directly in the grid. Inside any folder, you can also rearrange shows manually.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowsSortSection(
    currentSort: SubscriptionSort,
    onSortChange: (SubscriptionSort) -> Unit,
) {
    SectionHeader(
        icon = Icons.Rounded.Podcasts,
        title = "Shows Outside Folders",
        description = "How individual podcasts in your library grid are sorted",
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SortChip(
            label = "Smart Sort",
            selected = currentSort == SubscriptionSort.SmartRank,
            onClick = { onSortChange(SubscriptionSort.SmartRank) },
        )
        SortChip(
            label = "Recently Updated",
            selected = currentSort == SubscriptionSort.RecentlyUpdated,
            onClick = { onSortChange(SubscriptionSort.RecentlyUpdated) },
        )
        SortChip(
            label = "A–Z",
            selected = currentSort == SubscriptionSort.Alphabetical,
            onClick = { onSortChange(SubscriptionSort.Alphabetical) },
        )
        SortChip(
            label = "Most Listened",
            selected = currentSort == SubscriptionSort.MostListened,
            onClick = { onSortChange(SubscriptionSort.MostListened) },
        )
        SortChip(
            label = "Manual",
            selected = currentSort == SubscriptionSort.Manual,
            onClick = { onSortChange(SubscriptionSort.Manual) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderSortSection(
    folderSort: FolderInterSort,
    onFolderSortChange: (FolderInterSort) -> Unit,
) {
    SectionHeader(
        icon = Icons.Rounded.Folder,
        title = "Folders (Pinned at Top)",
        description = "How folders are ordered relative to each other",
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FolderInterSort.entries.forEach { option ->
            SortChip(
                label = option.label,
                selected = folderSort == option,
                onClick = { onFolderSortChange(option) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntraFolderSortSection(
    intraFolderSort: FolderIntraSort,
    onIntraFolderSortChange: (FolderIntraSort) -> Unit,
) {
    SectionHeader(
        icon = Icons.Rounded.FolderOpen,
        title = "Shows Inside Folders",
        description = "How shows appear inside cards and when opening a folder",
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FolderIntraSort.entries.forEach { option ->
            SortChip(
                label = option.label,
                selected = intraFolderSort == option,
                onClick = { onIntraFolderSortChange(option) },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = GoogleSansWeight.bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) GoogleSansWeight.bold else GoogleSansWeight.medium,
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        ),
    )
}

private fun formatShowsSortSummary(sort: SubscriptionSort): String = when (sort) {
    SubscriptionSort.SmartRank -> "Smart Rank"
    SubscriptionSort.RecentlyUpdated -> "Recently Updated"
    SubscriptionSort.Alphabetical -> "A–Z"
    SubscriptionSort.MostListened -> "Most Listened"
    SubscriptionSort.Manual -> "Manual Order"
}

private fun formatFolderInterSortSummary(folderSort: FolderInterSort, showText: String): String = when (folderSort) {
    FolderInterSort.Inherit -> "Follow Shows ($showText)"
    FolderInterSort.RecentlyUpdated -> "Recently Updated"
    FolderInterSort.Alphabetical -> "A–Z"
    FolderInterSort.MostShows -> "Most Shows"
    FolderInterSort.Manual -> "Manual Order"
}

private fun formatFolderIntraSortSummary(intraSort: FolderIntraSort, showText: String): String = when (intraSort) {
    FolderIntraSort.Inherit -> "Follow Shows ($showText)"
    FolderIntraSort.SmartRank -> "Smart Rank"
    FolderIntraSort.RecentlyUpdated -> "Recently Updated"
    FolderIntraSort.Alphabetical -> "A–Z"
    FolderIntraSort.MostListened -> "Most Listened"
    FolderIntraSort.Manual -> "Folder Order"
}

@Composable
private fun ActiveSortSummaryCard(
    currentSort: SubscriptionSort,
    folderSort: FolderInterSort,
    intraFolderSort: FolderIntraSort,
) {
    val showText = formatShowsSortSummary(currentSort)
    val folderText = formatFolderInterSortSummary(folderSort, showText)
    val intraText = formatFolderIntraSortSummary(intraFolderSort, showText)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Current arrangement",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = GoogleSansWeight.bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "• Shows outside folders: $showText\n• Folders at top: $folderText\n• Shows inside folders: $intraText",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutoOrganizeRow(
    autoOrganize: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Auto-organize into folders",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = GoogleSansWeight.medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Automatically place newly subscribed shows into matching genre folders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = autoOrganize,
            onCheckedChange = onToggle,
        )
    }
}
