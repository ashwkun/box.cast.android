package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.aswin.boxlore.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.core.model.FolderDisplaySize

@Composable
internal fun AutoOrganizeEnableDialog(
    onProceed: (displaySize: FolderDisplaySize?, showPodcastGrid: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedSize by remember { mutableStateOf<FolderDisplaySize?>(null) }
    var showPodcastGrid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        title = {
            Text(
                text = "Auto-Organize Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = GoogleSansWeight.bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Group your subscribed podcasts into folders by genre. Custom folders you've created stay safe and untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FolderSizeSelector(
                    selectedSize = selectedSize,
                    onSelectSize = { selectedSize = it },
                )

                if (selectedSize == FolderDisplaySize.COMPACT) {
                    CompactCoverStyleSelector(
                        showPodcastGrid = showPodcastGrid,
                        onSelectStyle = { showPodcastGrid = it },
                    )
                }

                GenreEditingTipCard()
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

@Composable
internal fun AutoOrganizeDisableDialog(
    onProceed: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        title = {
            Text(
                text = "Turn off auto-organize?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = GoogleSansWeight.bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Automatic background grouping will stop. Your current library remains completely safe:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DisableBenefitRow(
                            imageVector = Icons.Rounded.Check,
                            tint = MaterialTheme.colorScheme.primary,
                            title = "Existing folders stay intact",
                            subtitle = "None of your current folders or shows will be deleted",
                        )
                        DisableBenefitRow(
                            imageVector = Icons.Rounded.Podcasts,
                            tint = MaterialTheme.colorScheme.secondary,
                            title = "New shows stay in main grid",
                            subtitle = "Subsequent subscriptions appear directly in your library",
                        )
                        DisableBenefitRow(
                            imageVector = Icons.Rounded.TouchApp,
                            tint = MaterialTheme.colorScheme.tertiary,
                            title = "Manual control preserved",
                            subtitle = "Create, edit, or remove folders whenever you want",
                        )
                    }
                }
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

@Composable
private fun DisableBenefitRow(
    imageVector: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = GoogleSansWeight.bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FolderSizeSelector(
    selectedSize: FolderDisplaySize?,
    onSelectSize: (FolderDisplaySize?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "FOLDER SIZING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = GoogleSansWeight.bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp,
        )

        AutoFolderSizeHeroCard(
            isAutoSelected = selectedSize == null,
            onClick = { onSelectSize(null) },
        )

        Text(
            text = "Or choose a uniform size for all folders:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        UniformFolderSizeRow(
            selectedSize = selectedSize,
            onSelectSize = onSelectSize,
        )
    }
}

@Composable
private fun AutoFolderSizeHeroCard(
    isAutoSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        selected = isAutoSelected,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isAutoSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (isAutoSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAutoSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (isAutoSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Auto (Adaptive)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = GoogleSansWeight.bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = GoogleSansWeight.bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "1–2 shows: Compact • 3–5: Shelf • 6+: Panel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isAutoSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun UniformFolderSizeRow(
    selectedSize: FolderDisplaySize?,
    onSelectSize: (FolderDisplaySize) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            FolderDisplaySize.SHELF,
            FolderDisplaySize.COMPACT,
            FolderDisplaySize.PANEL,
            FolderDisplaySize.SHOWCASE,
        ).forEach { size ->
            UniformFolderSizeCard(
                size = size,
                isSelected = selectedSize == size,
                onClick = { onSelectSize(size) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UniformFolderSizeCard(
    size: FolderDisplaySize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = size.dimensionsLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = GoogleSansWeight.bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = size.title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun CompactCoverStyleSelector(
    showPodcastGrid: Boolean,
    onSelectStyle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "1×1 COVER STYLE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = GoogleSansWeight.bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                false to ("Folder Icon" to Icons.Rounded.Folder),
                true to ("Podcast Grid" to Icons.Rounded.Podcasts),
            ).forEach { (isGrid, info) ->
                val (label, icon) = info
                val isSelected = showPodcastGrid == isGrid
                Surface(
                    selected = isSelected,
                    onClick = { onSelectStyle(isGrid) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = if (isSelected) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) GoogleSansWeight.bold else GoogleSansWeight.medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreEditingTipCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tip: Change folder anytime",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap any show's genre pill on its info page to adjust its tag or move it to a different folder at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
