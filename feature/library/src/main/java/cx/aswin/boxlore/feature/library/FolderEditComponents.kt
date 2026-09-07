package cx.aswin.boxlore.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.aswin.boxlore.core.designsystem.components.PillFilterChip
import cx.aswin.boxlore.core.designsystem.icon.GenreIcons
import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion
import cx.aswin.boxlore.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.core.model.FolderDisplaySize

internal val DefaultFolderDisplaySize = FolderDisplaySize.SHELF

internal val SelectableFolderSizes = listOf(
    FolderDisplaySize.COMPACT,
    FolderDisplaySize.SHELF,
    FolderDisplaySize.PANEL,
    FolderDisplaySize.SHOWCASE,
)

internal data class FolderOrganizationState(
    val autoSync: Boolean,
    val onAutoSyncChange: (Boolean) -> Unit,
    val linkedGenre: String,
    val suggestedGenres: List<String> = emptyList(),
    val onSelectLinkedGenre: ((String) -> Unit)? = null,
    val autoOrganize: Boolean = false,
    val onAutoOrganizeChange: ((Boolean) -> Unit)? = null,
)

@Composable
internal fun FolderEditTopBar(
    isEditing: Boolean,
    canSave: Boolean,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = if (isEditing) "Edit folder" else "New folder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = GoogleSansWeight.bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isEditing && onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = GoogleSansWeight.medium,
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = canSave,
                shape = ExpressiveShapes.Pill,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (isEditing) "Save" else "Create",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.bold,
                )
            }
        }
    }
}

@Composable
internal fun FolderBetaFeedbackNotice(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Folders are still in beta — your feedback is greatly appreciated, especially if you find any bugs.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
internal fun FolderIdentityHeader(
    nameText: String,
    onNameChange: (String) -> Unit,
    iconKey: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = nameText,
        onValueChange = onNameChange,
        label = { Text("Folder name") },
        placeholder = { Text("e.g. Daily Tech, Comedy, Science") },
        leadingIcon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GenreIcons.folderIconOrFallback(iconKey),
                        contentDescription = "Folder icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        singleLine = true,
        trailingIcon = {
            if (nameText.isNotEmpty()) {
                IconButton(onClick = { onNameChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun FolderTechnologyWarningCard(
    onSwitchToTech: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp),
                )
                Text(
                    text = "The last dev who tried supporting 'Technology' as a folder name broke push notifications for three weeks and burned our server. Please, just 'Tech'. 📉",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = GoogleSansWeight.medium,
                )
            }

            Button(
                onClick = onSwitchToTech,
                shape = ExpressiveShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "Switch to 'Tech'",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.bold,
                )
            }
        }
    }
}

@Composable
internal fun FolderIconPickerRow(
    selectedIconKey: String?,
    queryText: String,
    onSelectIcon: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestedIcons = remember(queryText) {
        GenreIcons.suggestIcons(queryText)
    }
    val displayIcons = remember(suggestedIcons) {
        val suggestedKeys = suggestedIcons.map { it.key }.toSet()
        suggestedIcons + GenreIcons.all.filterNot { it.key in suggestedKeys }
    }

    val initialIndex = remember {
        val matchIndex = displayIcons.indexOfFirst { it.key.equals(selectedIconKey, ignoreCase = true) }
        if (matchIndex >= 0) matchIndex + 1 else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Folder icon (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!selectedIconKey.isNullOrBlank()) {
                    TextButton(
                        onClick = { onSelectIcon(null) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Reset to default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "default_folder_icon") {
                    FolderIconTile(
                        icon = Icons.Rounded.Folder,
                        label = "Default",
                        isSelected = selectedIconKey.isNullOrBlank(),
                        onClick = { onSelectIcon(null) },
                    )
                }

                items(displayIcons, key = { it.key }) { item ->
                    val isSelected = selectedIconKey.equals(item.key, ignoreCase = true)
                    FolderIconTile(
                        icon = item.icon,
                        label = item.label,
                        isSelected = isSelected,
                        onClick = { onSelectIcon(if (isSelected) null else item.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderIconTile(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun FolderQuickFillChipsRow(
    suggestions: List<GenreSuggestion>,
    queryText: String,
    hasLibraryGenres: Boolean,
    onSelectSuggestion: (GenreSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return

    val headerText = if (queryText.isBlank()) {
        if (hasLibraryGenres) "Suggested from your library" else "Suggested folders"
    } else {
        "Matching suggestions (${suggestions.size})"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = GoogleSansWeight.medium,
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = suggestions,
                key = { "${it.name}_${it.iconKey}_${it.isFromLibrary}" },
            ) { suggestion ->
                val isSelected = queryText.trim().equals(suggestion.name, ignoreCase = true)
                PillFilterChip(
                    label = suggestion.name,
                    icon = suggestion.icon,
                    selected = isSelected,
                    onClick = { onSelectSuggestion(suggestion) },
                )
            }
        }
    }
}

@Composable
internal fun FolderDisplaySizeSelector(
    selectedSize: FolderDisplaySize,
    onSizeSelected: (FolderDisplaySize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Folder size",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = GoogleSansWeight.bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectableFolderSizes.forEach { size ->
                FolderSizeCard(
                    size = size,
                    isSelected = selectedSize == size,
                    onClick = { onSizeSelected(size) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = selectedSize.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = GoogleSansWeight.medium,
            )
        }
    }
}

@Composable
private fun FolderSizeCard(
    size: FolderDisplaySize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = size.dimensionsLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = GoogleSansWeight.bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = size.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FolderCompactCoverStyleCard(
    showPodcastGrid: Boolean,
    hasIcon: Boolean,
    selectedIconKey: String?,
    onShowPodcastGridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIconSelected = hasIcon && !showPodcastGrid
    val isGridSelected = showPodcastGrid || !hasIcon
    val iconVector = GenreIcons.iconOrFallback(selectedIconKey, null)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "1×1 Cover style",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = GoogleSansWeight.bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoverStyleOptionTab(
                    title = "Folder Icon",
                    subtitle = if (hasIcon) "Tap to open" else "Pick icon above",
                    iconVector = iconVector,
                    isSelected = isIconSelected,
                    onClick = if (hasIcon) {
                        { onShowPodcastGridChange(false) }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )

                CoverStyleOptionTab(
                    title = "Podcast Grid",
                    subtitle = "2×2 covers",
                    iconVector = Icons.Rounded.GridView,
                    isSelected = isGridSelected,
                    onClick = { onShowPodcastGridChange(true) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CoverStyleOptionTab(
    title: String,
    subtitle: String,
    iconVector: ImageVector,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val enabled = onClick != null
    Surface(
        modifier = modifier.clickable(enabled = enabled) { onClick?.invoke() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = if (enabled) {
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = GoogleSansWeight.medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun FolderOrganizationCard(
    state: FolderOrganizationState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Organization",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = GoogleSansWeight.bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OrganizationSwitchRow(
                icon = Icons.Rounded.Sync,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = "Auto-sync with genre",
                subtitle = if (state.autoSync && state.linkedGenre.isNotBlank()) {
                    "Shows tagged '${state.linkedGenre}' join automatically"
                } else {
                    "Automatically adds matching subscribed shows"
                },
                checked = state.autoSync,
                onCheckedChange = state.onAutoSyncChange,
            )

            if (state.autoSync && state.suggestedGenres.isNotEmpty() && state.onSelectLinkedGenre != null) {
                AutoSyncGenreChips(
                    suggestedGenres = state.suggestedGenres,
                    linkedGenre = state.linkedGenre,
                    onSelectLinkedGenre = state.onSelectLinkedGenre,
                )
            }

            if (state.onAutoOrganizeChange != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )

                OrganizationSwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Auto-organize library",
                    subtitle = if (state.autoOrganize) {
                        "Library automatically grouped by genre"
                    } else {
                        "Group all shows into genre folders automatically"
                    },
                    checked = state.autoOrganize,
                    onCheckedChange = state.onAutoOrganizeChange,
                )
            }
        }
    }
}

@Composable
private fun OrganizationSwitchRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = GoogleSansWeight.medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AutoSyncGenreChips(
    suggestedGenres: List<String>,
    linkedGenre: String,
    onSelectLinkedGenre: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(suggestedGenres, key = { it }) { genre ->
            val isSelected = linkedGenre.equals(genre, ignoreCase = true)
            val icon = GenreIcons.iconOrFallback(null, genre)
            PillFilterChip(
                label = genre,
                icon = icon,
                selected = isSelected,
                onClick = { onSelectLinkedGenre(genre) },
            )
        }
    }
}
