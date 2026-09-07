package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.aswin.boxlore.core.designsystem.components.OptimizedImage
import cx.aswin.boxlore.core.designsystem.icon.GenreIcons
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder

/**
 * Pinned enlarged folder card spanning full grid width (SHELF 3×1, PANEL 3×2, SHOWCASE 3×3).
 * Shows inside the card are directly clickable to open podcast details.
 * The overflow slot or header opens the full folder view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PinnedEnlargedFolderCard(
    folder: SubscriptionFolder,
    podcasts: List<Podcast>,
    onPodcastClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (SubscriptionFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val folderIcon = GenreIcons.folderIconOrFallback(folder.icon)
    val slots = calculateFolderSlots(podcasts, folder.displaySize)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFolderClick(folder.id) },
                onLongClick = { onFolderLongClick(folder) },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: Icon, Folder Name, Show Count, Chevron (Click to expand folder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onFolderClick(folder.id) },
                        onLongClick = { onFolderLongClick(folder) },
                    )
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = folderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = GoogleSansWeight.bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (podcasts.size == 1) "1 show" else "${podcasts.size} shows",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Open folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Grid of directly clickable covers
            if (podcasts.isEmpty()) {
                EmptyFolderPlaceholder(
                    onAddShowsClick = { onFolderClick(folder.id) },
                )
            } else {
                FolderCoversGrid(
                    slots = slots,
                    columns = folder.displaySize.spanCols,
                    onPodcastClick = onPodcastClick,
                    onOverflowClick = { onFolderClick(folder.id) },
                )
            }
        }
    }
}

/**
 * Grid of directly clickable covers inside an enlarged folder card.
 */
@Composable
private fun FolderCoversGrid(
    slots: FolderSlots,
    columns: Int,
    onPodcastClick: (String) -> Unit,
    onOverflowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalItems = slots.visibleShows.size + if (slots.hasOverflow) 1 else 0
    val rows = (totalItems + columns - 1) / columns

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (colIndex in 0 until columns) {
                    val itemIndex = rowIndex * columns + colIndex
                    if (itemIndex < slots.visibleShows.size) {
                        val podcast = slots.visibleShows[itemIndex]
                        DirectClickableShowCover(
                            podcast = podcast,
                            onClick = { onPodcastClick(podcast.id) },
                            modifier = Modifier.weight(1f),
                        )
                    } else if (itemIndex == slots.visibleShows.size && slots.hasOverflow) {
                        FolderOverflowSlotCard(
                            overflowShows = slots.overflowShows,
                            overflowCount = slots.overflowCount,
                            onClick = onOverflowClick,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(onClick = onOverflowClick),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single directly clickable podcast cover within a folder card.
 */
@Composable
private fun DirectClickableShowCover(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick),
    ) {
        OptimizedImage(
            url = podcast.imageUrl.takeIf { it.isNotEmpty() } ?: podcast.fallbackImageUrl,
            proxyWidth = 240,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            errorContent = {
                ArtworkTitleFallback(title = podcast.title)
            },
        )
    }
}

/**
 * Overflow slot representing a 2×2 mini cluster of remaining shows with "+N" badge.
 */
@Composable
private fun FolderOverflowSlotCard(
    overflowShows: List<Podcast>,
    overflowCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 2×2 mini collage preview of overflow shows
        val previewShows = overflowShows.take(4)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                previewShows.getOrNull(0)?.let { MiniCover(it, Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
                previewShows.getOrNull(1)?.let { MiniCover(it, Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                previewShows.getOrNull(2)?.let { MiniCover(it, Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
                previewShows.getOrNull(3)?.let { MiniCover(it, Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
            }
        }

        // Dark scrim + bold count
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+$overflowCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = GoogleSansWeight.bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun MiniCover(podcast: Podcast, modifier: Modifier = Modifier) {
    OptimizedImage(
        url = podcast.imageUrl.takeIf { it.isNotEmpty() } ?: podcast.fallbackImageUrl,
        proxyWidth = 100,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * 1×1 compact card supporting dual styles:
 * 1. Single Folder Icon style (prominent folder icon over background, single tap expands).
 * 2. 2×2 Podcast Grid style (mini covers directly clickable, overflow badge expands).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Compact1x1FolderCard(
    folder: SubscriptionFolder,
    podcasts: List<Podcast>,
    onPodcastClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (SubscriptionFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        if (!folder.effectiveShowPodcastGrid) {
            CompactFolderIconContent(
                folder = folder,
                podcastCount = podcasts.size,
                onFolderClick = onFolderClick,
                onFolderLongClick = onFolderLongClick,
            )
        } else {
            CompactPodcastGridContent(
                folder = folder,
                podcasts = podcasts,
                onPodcastClick = onPodcastClick,
                onFolderClick = onFolderClick,
                onFolderLongClick = onFolderLongClick,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactFolderIconContent(
    folder: SubscriptionFolder,
    podcastCount: Int,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (SubscriptionFolder) -> Unit,
) {
    val folderIcon = GenreIcons.folderIconOrFallback(folder.icon)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { onFolderClick(folder.id) },
                onLongClick = { onFolderLongClick(folder) },
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = folderIcon,
                contentDescription = folder.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = GoogleSansWeight.bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "$podcastCount shows",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactPodcastGridContent(
    folder: SubscriptionFolder,
    podcasts: List<Podcast>,
    onPodcastClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (SubscriptionFolder) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { onFolderClick(folder.id) },
                onLongClick = { onFolderLongClick(folder) },
            )
            .padding(5.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MiniPodcastSlot(
                    podcast = podcasts.getOrNull(0),
                    onClick = onPodcastClick,
                    onEmptyClick = { onFolderClick(folder.id) },
                    modifier = Modifier.weight(1f),
                )
                MiniPodcastSlot(
                    podcast = podcasts.getOrNull(1),
                    onClick = onPodcastClick,
                    onEmptyClick = { onFolderClick(folder.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MiniPodcastSlot(
                    podcast = podcasts.getOrNull(2),
                    onClick = onPodcastClick,
                    onEmptyClick = { onFolderClick(folder.id) },
                    modifier = Modifier.weight(1f),
                )
                if (podcasts.size > 4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onFolderClick(folder.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+${podcasts.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = GoogleSansWeight.bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    MiniPodcastSlot(
                        podcast = podcasts.getOrNull(3),
                        onClick = onPodcastClick,
                        onEmptyClick = { onFolderClick(folder.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPodcastSlot(
    podcast: Podcast?,
    onClick: (String) -> Unit,
    onEmptyClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val miniShape = RoundedCornerShape(6.dp)
    if (podcast != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(miniShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { onClick(podcast.id) },
        ) {
            OptimizedImage(
                url = podcast.imageUrl.takeIf { it.isNotEmpty() } ?: podcast.fallbackImageUrl,
                proxyWidth = 140,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(miniShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                .clickable(onClick = onEmptyClick),
        )
    }
}

@Composable
private fun EmptyFolderPlaceholder(
    onAddShowsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
            .clickable(onClick = onAddShowsClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Folder is empty • Tap to add shows",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = GoogleSansWeight.medium,
            )
        }
    }
}
