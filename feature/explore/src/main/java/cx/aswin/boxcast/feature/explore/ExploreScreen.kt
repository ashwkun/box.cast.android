package cx.aswin.boxcast.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import cx.aswin.boxcast.core.designsystem.components.AnimatedShapesFallback
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Podcast

import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Main Explore Screen Entry Point
 */
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onPodcastClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExploreContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onPodcastClick = onPodcastClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreContent(
    uiState: ExploreUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPodcastClick: (String) -> Unit
) {
    // explicitly handle state to ensure screen structure exists for transitions
    when (uiState) {
        is ExploreUiState.Loading -> {
             // Show Skeleton State
             Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
             ) {
                // Header Skeleton
                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainerHigh)) // Search Bar
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         repeat(4) {
                             Box(modifier = Modifier.width(80.dp).height(32.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer))
                         }
                    }
                 }
                 
                 // Grid Skeleton
                 LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                         Box(modifier = Modifier.width(200.dp).height(24.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer))
                         Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(6) { 
                         Column {
                             Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer))
                             Spacer(modifier = Modifier.height(8.dp))
                             Box(modifier = Modifier.width(100.dp).height(16.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainer))
                         }
                    }
                }
             }
             return
        }
        is ExploreUiState.Error -> {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 Text(text = "Error: ${(uiState as ExploreUiState.Error).message}")
             }
             return
        }
        is ExploreUiState.Success -> {
            // Proceed to render content
        }
    }
    
    val state = uiState as ExploreUiState.Success

    // Scroll state for collapsing logic
    var isGenreVisible by rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // available.y > 0 -> Scrolling Down (Showing top) -> Show Genre
                // available.y < 0 -> Scrolling Up (Going deeper) -> Hide Genre
                if (available.y < -5f) { // Swipe Up
                    isGenreVisible = false
                } else if (available.y > 5f) { // Swipe Down
                    isGenreVisible = true
                }
                return androidx.compose.ui.geometry.Offset.Zero // Don't consume the scroll
            }
        }
    }

    var active by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Persistent Header Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar (Always Visible)
            DockedSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onSearch = { active = false },
                active = false,
                onActiveChange = { active = it },
                placeholder = { Text("Search podcasts...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) { }
            
            // Collapsible Genre Tabs
            AnimatedVisibility(
                visible = isGenreVisible,
                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(300)) + fadeIn(),
                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(300)) + fadeOut()
            ) {
                 Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val categories = listOf("All", "News", "Technology", "Comedy", "True Crime", "Business", "Sports", "History", "Arts")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = state.currentCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.expressiveClickable(onClick = { onCategorySelected(category) })
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Small buffer when expanded
                 }
            }
        }

        // 3. Main Grid (Takes remaining space)
        val displayList = if (state.isSearching) state.searchResults else state.trending

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f) // Fill remaining space
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = 16.dp, // Top padding from header
                    bottom = 80.dp // Bottom nav padding
                ) 
            ) {
                // Header Span (Title Only)
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = if (state.isSearching) "Search Results" else "Trending in ${state.currentCategory}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                itemsIndexed(
                    items = displayList,
                    span = { index, _ ->
                        if (!state.isSearching && index < 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
                    }
                ) { index, podcast ->
                    val isSubscribed = state.subscribedIds.contains(podcast.id)
                    if (!state.isSearching && index < 2) {
                        ExploreHeroCard(
                            podcast = podcast,
                            isSubscribed = isSubscribed,
                            onClick = { onPodcastClick(podcast.id) }
                        )
                    } else {
                        ExploreCompactCard(
                            podcast = podcast,
                            isSubscribed = isSubscribed,
                            onClick = { onPodcastClick(podcast.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreHeroCard(
    podcast: Podcast,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .expressiveClickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            SubcomposeAsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { AnimatedShapesFallback() },
                error = { AnimatedShapesFallback() }
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, 
                            Color.Black.copy(alpha = 0.5f), 
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    ))
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = podcast.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Subscribed Badge
            if (isSubscribed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Subscribed",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreCompactCard(
    podcast: Podcast,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large) // Match card shape for top corners at least? Or consistent rounding
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                SubcomposeAsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { AnimatedShapesFallback() },
                    error = { AnimatedShapesFallback() }
                )
                
                if (isSubscribed) {
                     Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Subscribed",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = podcast.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
