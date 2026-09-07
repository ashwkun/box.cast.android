package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cx.aswin.boxlore.core.designsystem.components.NewEpisodeBadge
import cx.aswin.boxlore.core.designsystem.components.OptimizedImage
import cx.aswin.boxlore.core.designsystem.icon.GenreIcons
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.core.model.isLatestEpisodeNew
import cx.aswin.boxlore.feature.library.LocalLastSeenEpisodes

/**
 * Callbacks for interacting with folder cards in the Subscriptions grid.
 */
internal data class FolderCardActions(
    val onPodcastClick: (String) -> Unit,
    val onFolderClick: (String) -> Unit,
    val onFolderLongClick: (SubscriptionFolder) -> Unit = {},
)

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
    actions: FolderCardActions,
    modifier: Modifier = Modifier,
) {
    val lastSeenEpisodes = LocalLastSeenEpisodes.current
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
                onClick = { actions.onFolderClick(folder.id) },
                onLongClick = { actions.onFolderLongClick(folder) },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PinnedFolderHeader(
                folder = folder,
                podcastsCount = podcasts.size,
                folderIcon = folderIcon,
                onFolderClick = actions.onFolderClick,
                onFolderLongClick = actions.onFolderLongClick,
            )

            // Grid of directly clickable covers
            if (podcasts.isEmpty()) {
                EmptyFolderPlaceholder(
                    onAddShowsClick = { actions.onFolderClick(folder.id) },
                )
            } else {
                FolderCoversGrid(
                    slots = slots,
                    columns = folder.displaySize.spanCols,
                    lastSeenEpisodes = lastSeenEpisodes,
                    onPodcastClick = actions.onPodcastClick,
                    onOverflowClick = { actions.onFolderClick(folder.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedFolderHeader(
    folder: SubscriptionFolder,
    podcastsCount: Int,
    folderIcon: ImageVector,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (SubscriptionFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
                text = if (podcastsCount == 1) "1 show" else "$podcastsCount shows",
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
}

/**
 * Grid of directly clickable covers inside an enlarged folder card.
 */
@Composable
private fun FolderCoversGrid(
    slots: FolderSlots,
    columns: Int,
    lastSeenEpisodes: Map<String, String>,
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
                        val isNew = remember(podcast, lastSeenEpisodes) {
                            podcast.isLatestEpisodeNew(lastSeenEpisodes[podcast.id])
                        }
                        DirectClickableShowCover(
                            podcast = podcast,
                            hasNewEpisode = isNew,
                            onClick = { onPodcastClick(podcast.id) },
                            modifier = Modifier.weight(1f),
                        )
                    } else if (itemIndex == slots.visibleShows.size && slots.hasOverflow) {
                        val hasOverflowNew = remember(slots.overflowShows, lastSeenEpisodes) {
                            hasFolderOverflowNew(slots.overflowShows, lastSeenEpisodes)
                        }
                        FolderOverflowSlotCard(
                            overflowShows = slots.overflowShows,
                            overflowCount = slots.overflowCount,
                            hasNewEpisode = hasOverflowNew,
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
    hasNewEpisode: Boolean,
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
        if (hasNewEpisode) {
            NewEpisodeBadge()
        }
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
    hasNewEpisode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (hasNewEpisode) {
                    Modifier.border(
                        BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        shape,
                    )
                } else {
                    Modifier
                }
            )
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

        if (hasNewEpisode) {
            NewEpisodeBadge()
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
    actions: FolderCardActions,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
) {
    val lastSeenEpisodes = LocalLastSeenEpisodes.current
    val hasOverflowNew = remember(podcasts, lastSeenEpisodes) {
        if (podcasts.size > 4) {
            hasFolderOverflowNew(podcasts.drop(3), lastSeenEpisodes)
        } else {
            false
        }
    }
    val hasAnyNew = remember(podcasts, lastSeenEpisodes) {
        hasAnyFolderShowNew(podcasts, lastSeenEpisodes)
    }
    val showFolderBadge = if (folder.effectiveShowPodcastGrid) {
        hasOverflowNew
    } else {
        hasAnyNew
    }
    val shape = RoundedCornerShape(14.dp)
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        label = "compactFolderDragScale",
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "compactFolderDragElevation",
    )

    Box(
        modifier = modifier
            .then(dragModifier)
            .fillMaxWidth()
            .aspectRatio(1f)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
                this.shape = shape
                clip = false
            }
            .shadow(elevation = dragElevation, shape = shape, clip = false),
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (showFolderBadge) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!folder.effectiveShowPodcastGrid) {
                CompactFolderIconContent(
                    folder = folder,
                    podcastCount = podcasts.size,
                    onFolderClick = actions.onFolderClick,
                    onFolderLongClick = actions.onFolderLongClick,
                )
            } else {
                CompactPodcastGridContent(
                    folder = folder,
                    podcasts = podcasts,
                    lastSeenEpisodes = lastSeenEpisodes,
                    actions = actions,
                )
            }
        }

        if (showFolderBadge) {
            FolderFloatingBadge()
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
    lastSeenEpisodes: Map<String, String>,
    actions: FolderCardActions,
) {
    val pod0 = podcasts.getOrNull(0)
    val isNew0 = remember(pod0, lastSeenEpisodes) {
        pod0?.let { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) } ?: false
    }
    val pod1 = podcasts.getOrNull(1)
    val isNew1 = remember(pod1, lastSeenEpisodes) {
        pod1?.let { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) } ?: false
    }
    val pod2 = podcasts.getOrNull(2)
    val isNew2 = remember(pod2, lastSeenEpisodes) {
        pod2?.let { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) } ?: false
    }
    val pod3 = podcasts.getOrNull(3)
    val isNew3 = remember(pod3, lastSeenEpisodes) {
        pod3?.let { it.isLatestEpisodeNew(lastSeenEpisodes[it.id]) } ?: false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { actions.onFolderClick(folder.id) },
                onLongClick = { actions.onFolderLongClick(folder) },
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
                    podcast = pod0,
                    hasNewEpisode = isNew0,
                    onClick = actions.onPodcastClick,
                    onEmptyClick = { actions.onFolderClick(folder.id) },
                    modifier = Modifier.weight(1f),
                )
                MiniPodcastSlot(
                    podcast = pod1,
                    hasNewEpisode = isNew1,
                    onClick = actions.onPodcastClick,
                    onEmptyClick = { actions.onFolderClick(folder.id) },
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
                    podcast = pod2,
                    hasNewEpisode = isNew2,
                    onClick = actions.onPodcastClick,
                    onEmptyClick = { actions.onFolderClick(folder.id) },
                    modifier = Modifier.weight(1f),
                )
                if (podcasts.size > 4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { actions.onFolderClick(folder.id) },
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
                        podcast = pod3,
                        hasNewEpisode = isNew3,
                        onClick = actions.onPodcastClick,
                        onEmptyClick = { actions.onFolderClick(folder.id) },
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
    hasNewEpisode: Boolean = false,
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
            if (hasNewEpisode) {
                MiniNewEpisodeBadge()
            }
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
private fun BoxScope.MiniNewEpisodeBadge(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(top = 2.dp, end = 2.dp),
    ) {
        Text(
            text = "NEW",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.sp,
                fontWeight = GoogleSansWeight.extraBold,
                letterSpacing = 0.3.sp,
                lineHeight = 7.sp,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun BoxScope.FolderFloatingBadge(
    modifier: Modifier = Modifier,
) {
    NewEpisodeBadge(
        modifier = modifier
            .offset(x = 8.dp, y = (-8).dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(6.dp))
            .zIndex(10f),
    )
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
