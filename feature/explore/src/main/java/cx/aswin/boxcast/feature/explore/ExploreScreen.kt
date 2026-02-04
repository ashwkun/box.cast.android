package cx.aswin.boxcast.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import cx.aswin.boxcast.core.designsystem.components.AnimatedShapesFallback
import cx.aswin.boxcast.core.designsystem.theme.SectionHeaderFontFamily
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.feature.explore.components.ExploreSkeletonLoader
import cx.aswin.boxcast.feature.explore.components.exploreSkeletonGridItems

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
    // Handle error/loading states
    when (uiState) {
        is ExploreUiState.Loading -> {
            ExploreSkeletonLoader()
            return
        }
        is ExploreUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${uiState.message}")
            }
            return
        }
        is ExploreUiState.Success -> { /* Continue */ }
    }

    val state = uiState as ExploreUiState.Success
    val displayList = if (state.isSearching) state.searchResults else state.trending
    
    // Scroll-driven genre collapse (like original)
    var isGenreVisible by rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5f) isGenreVisible = false
                else if (available.y > 5f) isGenreVisible = true
                return Offset.Zero
            }
        }
    }

    var searchActive by rememberSaveable { mutableStateOf(false) }

    // Column layout to keep search bar fixed at top
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .nestedScroll(nestedScrollConnection)
    ) {
        // FIXED HEADER: Search + Genre Chips
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
                onSearch = { searchActive = false },
                active = false,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Search podcasts...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) { }
            
            // Collapsible Genre Chips (like original)
            AnimatedVisibility(
                visible = isGenreVisible && !state.isSearching,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExploreGenreSelector(
                        selectedCategory = state.currentCategory,
                        onCategorySelected = onCategorySelected
                    )
                }
            }
            
            // Bottom spacing (always visible - prevents cut-off look)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // SCROLLABLE CONTENT: Grid
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) {
            // Featured Hero Row (Top 3, only when not searching and has content)
            if (!state.isSearching && displayList.size >= 3 && !state.isLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ExploreFeaturedRow(
                        podcasts = displayList.take(3),
                        onPodcastClick = onPodcastClick,
                        showGenreChip = state.currentCategory == "All"
                    )
                }
            }

            // Section Header
            item(span = StaggeredGridItemSpan.FullLine) {
                ExploreSectionHeader(
                    title = if (state.isSearching) "Search Results" else "Trending in ${state.currentCategory}"
                )
            }

            // Content (skip first 3 if showing featured row)
            if (state.isLoading || (displayList.isEmpty() && !state.isSearching)) {
                exploreSkeletonGridItems()
            } else if (displayList.isEmpty() && state.isSearching) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ExploreEmptyState()
                }
            } else {
                val gridItems = if (!state.isSearching && displayList.size >= 3) displayList.drop(3) else displayList
                val showGenreChip = state.currentCategory == "All" // Only show chips for "All" tab
                items(gridItems, key = { "${gridItems.indexOf(it)}_${it.id}" }) { podcast ->
                    val isTall = podcast.id.hashCode() % 3 == 0
                    ExplorePodcastCard(
                        podcast = podcast,
                        isTall = isTall,
                        showGenreChip = showGenreChip,
                        onClick = { onPodcastClick(podcast.id) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTS - Matching HomeScreen exactly
// ============================================================================

/**
 * Genre selector matching HomeScreen's GenreSelector exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreGenreSelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "News", "Technology", "Comedy", "True Crime", "Business", "Sports", "History", "Arts")
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ),
                modifier = Modifier.expressiveClickable(onClick = { onCategorySelected(category) })
            )
        }
    }
}

/**
 * Featured hero row - horizontal scrolling spotlight of top 3 podcasts
 */
@Composable
private fun ExploreFeaturedRow(
    podcasts: List<Podcast>,
    onPodcastClick: (String) -> Unit,
    showGenreChip: Boolean = false
) {
    Column {
        // Section header for featured
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Featured",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SectionHeaderFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Horizontal carousel of featured cards (matching Home hero style)
        val carouselState = rememberCarouselState { podcasts.size }
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 280.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(end = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) { index ->
            val podcast = podcasts[index]
            ExploreFeaturedCard(
                podcast = podcast,
                showGenreChip = showGenreChip,
                onClick = { onPodcastClick(podcast.id) },
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Featured podcast card - compact hero style with gradient overlay
 */
@Composable
private fun ExploreFeaturedCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showGenreChip: Boolean = false
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .width(280.dp)
            .height(170.dp)
            .expressiveClickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            SubcomposeAsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading ||
                    state is AsyncImagePainter.State.Error ||
                    podcast.imageUrl.isEmpty()) {
                    AnimatedShapesFallback()
                } else {
                    SubcomposeAsyncImageContent()
                }
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Genre Badge (Top Left) - only if showGenreChip is true
            if (showGenreChip && podcast.genre.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = podcast.genre.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Content (Bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = podcast.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Section header matching HomeScreen's DiscoverSection header
 */
@Composable
private fun ExploreSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = SectionHeaderFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Podcast card matching HomeScreen's PodcastCard exactly
 */
@Composable
fun ExplorePodcastCard(
    podcast: Podcast,
    isTall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showGenreChip: Boolean = false
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.expressiveClickable(onClick = onClick)
    ) {
        Column {
            // Image Container with optional Genre Chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTall) 280.dp else 220.dp)
            ) {
                SubcomposeAsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading ||
                        state is AsyncImagePainter.State.Error ||
                        podcast.imageUrl.isEmpty()) {
                        AnimatedShapesFallback()
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
                
                // Genre Chip (only shown when showGenreChip is true)
                if (showGenreChip && podcast.genre.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = podcast.genre.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Text content below image
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = podcast.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExploreEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No podcasts found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try a different search term",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
