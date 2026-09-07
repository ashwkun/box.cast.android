package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.core.model.FolderDisplaySize

@Composable
internal fun AutoOrganizeEnableDialog(
    onProceed: (displaySize: FolderDisplaySize?, showPodcastGrid: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedSize by remember { mutableStateOf<FolderDisplaySize?>(FolderDisplaySize.SHELF) }
    var showPodcastGrid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Auto-organize into folders?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = GoogleSansWeight.bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Group subscribed podcasts into folders by genre. Any custom folders you've created will stay untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FolderSizeChipSelector(
                    selectedSize = selectedSize,
                    onSelectSize = { selectedSize = it },
                )

                if (selectedSize == FolderDisplaySize.COMPACT) {
                    CompactCoverStyleChipSelector(
                        showPodcastGrid = showPodcastGrid,
                        onSelectStyle = { showPodcastGrid = it },
                    )
                }

                Text(
                    text = "Tip: You can edit any show's genre from its info page by tapping the genre pill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onProceed(selectedSize, showPodcastGrid) },
                shape = ExpressiveShapes.Pill,
            ) {
                Text("Proceed", fontWeight = GoogleSansWeight.bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
            ) {
                Text("Cancel", fontWeight = GoogleSansWeight.medium)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderSizeChipSelector(
    selectedSize: FolderDisplaySize?,
    onSelectSize: (FolderDisplaySize?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Folder size",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = GoogleSansWeight.bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedSize == null,
                onClick = { onSelectSize(null) },
                label = { Text("Auto", fontWeight = GoogleSansWeight.medium) },
                leadingIcon = if (selectedSize == null) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
            )

            listOf(
                FolderDisplaySize.SHELF to "3×1 Shelf",
                FolderDisplaySize.COMPACT to "1×1 Compact",
                FolderDisplaySize.PANEL to "3×2 Panel",
                FolderDisplaySize.SHOWCASE to "3×3 Showcase",
            ).forEach { (size, label) ->
                val isSelected = selectedSize == size
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectSize(size) },
                    label = { Text(label, fontWeight = GoogleSansWeight.medium) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactCoverStyleChipSelector(
    showPodcastGrid: Boolean,
    onSelectStyle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "1×1 cover style",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = GoogleSansWeight.bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showPodcastGrid,
                onClick = { onSelectStyle(false) },
                label = { Text("Folder Icon", fontWeight = GoogleSansWeight.medium) },
                leadingIcon = if (!showPodcastGrid) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
            )
            FilterChip(
                selected = showPodcastGrid,
                onClick = { onSelectStyle(true) },
                label = { Text("Podcast Grid", fontWeight = GoogleSansWeight.medium) },
                leadingIcon = if (showPodcastGrid) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
internal fun AutoOrganizeDisableDialog(
    onProceed: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Turn off auto-organize?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = GoogleSansWeight.bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Automatic grouping will stop. Existing folders and shows will stay in your library, and new subscriptions will appear directly in your main grid.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "You can still create, organize, and edit folders at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onProceed,
                shape = ExpressiveShapes.Pill,
            ) {
                Text("Turn Off", fontWeight = GoogleSansWeight.bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
            ) {
                Text("Cancel", fontWeight = GoogleSansWeight.medium)
            }
        },
    )
}
