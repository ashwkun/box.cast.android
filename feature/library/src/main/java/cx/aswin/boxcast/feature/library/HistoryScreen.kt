package cx.aswin.boxcast.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.data.database.ListeningHistoryEntity
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onEpisodeClick: (ListeningHistoryEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text("Listening History", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is HistoryUiState.Success) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Rounded.ClearAll, contentDescription = "Clear All")
                        }

                        if (showClearDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearDialog = false },
                                title = { Text("Clear History") },
                                text = { Text("Are you sure you want to clear your entire listening history? This cannot be undone.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showClearDialog = false
                                            viewModel.clearAllHistory()
                                        }
                                    ) {
                                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HistoryUiState.Empty -> {
                    ExpressiveSolarSystemEmptyState(
                        title = "No Listening History",
                        description = "Jump into a podcast and your history will magically appear here.",
                        icon = Icons.Rounded.History,
                        actionText = "Go Explore",
                        onExploreClick = onBack
                    )
                }
                is HistoryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Rich Stats Hero Section
                        item {
                            RichStatsDashboard(stats = state.stats)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Chronological Timeline
                        state.groupedHistory.forEach { (date, episodes) ->
                            val isExpanded = state.expandedDates.contains(date)

                            stickyHeader(key = date.toString()) {
                                DateHeaderRow(
                                    date = date,
                                    isExpanded = isExpanded,
                                    onClick = { viewModel.toggleDateExpansion(date) }
                                )
                            }

                            item(key = "anim_${date.toString()}") {
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = tween(400)),
                                    exit = shrinkVertically(animationSpec = tween(400))
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                                    ) {
                                        episodes.forEach { entity ->
                                            SwipeToDeleteHistoryItem(
                                                entity = entity,
                                                onDelete = { viewModel.removeHistoryItem(entity.episodeId) },
                                                onClick = { onEpisodeClick(entity) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichStatsDashboard(stats: RichHistoryStats) {
    // Bento-style Grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Large Tile: Total Listening Time
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Energetic expressive background graphic
                Box(
                    modifier = Modifier
                        .fillMaxSize(1.3f)
                        .offset(x = (-30).dp, y = 30.dp)
                        .clip(ExpressiveShapes.Puffy)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val hours = TimeUnit.MILLISECONDS.toHours(stats.totalListeningMs)
                    val mins = TimeUnit.MILLISECONDS.toMinutes(stats.totalListeningMs) % 60
                    
                    Text(
                        text = "${hours}h ${mins}m",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Two smaller tiles stacked vertically
        Column(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(1.5f)
                            .offset(x = 40.dp, y = (-20).dp)
                            .clip(ExpressiveShapes.Cookie4)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f))
                    )
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                        Column {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${stats.completedEpisodesCount} Eps",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(1.3f)
                            .offset(x = (-10).dp, y = (-30).dp)
                            .clip(ExpressiveShapes.Diamond)
                            .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.08f))
                    )
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Top Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stats.topPodcastName ?: "None",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        if (stats.topPodcastImageUrl != null) {
                            AsyncImage(
                                model = stats.topPodcastImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeaderRow(
    date: LocalDate,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    val dateText = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(formatter)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Caret rotation can be added here if desired.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteHistoryItem(
    entity: ListeningHistoryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = Modifier
            .fillMaxWidth()
            .expressiveClickable(shape = MaterialTheme.shapes.medium, onClick = onClick),
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium, // Staggered list items
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    AsyncImage(
                        model = entity.episodeImageUrl ?: entity.podcastImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Simple progress scrim on the image
                    if (entity.durationMs > 0) {
                        val progress = (entity.progressMs.toFloat() / entity.durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.episodeTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entity.podcastName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
