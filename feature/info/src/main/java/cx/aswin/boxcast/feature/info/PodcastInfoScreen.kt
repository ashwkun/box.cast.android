package cx.aswin.boxcast.feature.info

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.Html
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import cx.aswin.boxcast.core.designsystem.component.ExpressiveExtendedFab
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader
import cx.aswin.boxcast.core.designsystem.components.AnimatedShapesFallback
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Episode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.SubcomposeAsyncImage

private fun stripHtml(html: String?): String {
    if (html.isNullOrEmpty()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

private fun extractDominantColor(bitmap: Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val colorInt = palette.vibrantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: return Color.Transparent
    return Color(colorInt)
}

// Navbar height constant
private val NAVBAR_HEIGHT = 80.dp

// M3 Expressive Easing (Standard decelerate curve)
private val ExpressiveEasing = androidx.compose.animation.core.CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PodcastInfoScreen(
    podcastId: String,
    viewModel: PodcastInfoViewModel,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Use theme primary color (no dynamic extraction)
    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(podcastId) {
        viewModel.loadPodcast(podcastId)
    }
    
    // Scroll state for floating title animation (like Episode Info)
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 1000f // Fully collapsed
        }
    }
    
    // Scroll fraction: 0 (expanded) -> 1 (collapsed)
    val density = LocalDensity.current
    val morphThreshold = with(density) { 150.dp.toPx() }
    val scrollFraction = (scrollOffset / morphThreshold).coerceIn(0f, 1f)
    
    // Header dimensions
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapsedHeaderHeight = 64.dp + statusBarHeight
    
    // Header background: transparent → surfaceContainer
    // NOTE: Don't lerp from Color.Transparent - it has RGB=0,0,0 causing black flash
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val headerColor by animateColorAsState(
        targetValue = surfaceColor.copy(alpha = scrollFraction),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "headerColor"
    )
    
    // Title animation - floating title like Episode Info
    val titleSizeStart = MaterialTheme.typography.headlineSmall.fontSize
    val titleSizeEnd = MaterialTheme.typography.titleMedium.fontSize
    val titleFontSize = androidx.compose.ui.unit.lerp(titleSizeStart, titleSizeEnd, scrollFraction)
    
    // Y position: starts below header (above hero), ends in header
    val bodyTitleYPx = with(density) { collapsedHeaderHeight.toPx() + 16.dp.toPx() }
    val headerTitleYPx = with(density) { (statusBarHeight + 18.dp).toPx() }
    val titleTranslationY by animateFloatAsState(
        targetValue = androidx.compose.ui.util.lerp(bodyTitleYPx, headerTitleYPx, scrollFraction),
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f),
        label = "titleY"
    )
    
    // MaxLines - 3 when expanded, 1 when collapsed (change at 70% for late transition)
    val titleMaxLines = if (scrollFraction < 0.7f) 3 else 1
    // Keep alpha at 1 throughout - no fade discontinuity
    val titleAlpha = 1f
    
    // Horizontal padding
    val titleHorizontalPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(20.dp, 56.dp, scrollFraction),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "titlePadding"
    )

    // Scaffold removed - using Box overlay structure below for correct Edge-to-Edge behavior
    
    // REWRITE: Structure using Box to allow Overlay
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is PodcastInfoUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BoxCastLoader.Expressive(size = 80.dp)
                }
            }
            
            is PodcastInfoUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load podcast", color = MaterialTheme.colorScheme.error)
                }
            }
            
            is PodcastInfoUiState.Success -> {
                // Content
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                    contentPadding = PaddingValues(
                        top = collapsedHeaderHeight + 90.dp, // Header + space for 3-line floating title
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO SECTION: Card with Compact Row (Image + Metadata)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Small Shaped Image (Left)
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = MaterialTheme.shapes.large,
                                    shadowElevation = 4.dp
                                ) {
                                    SubcomposeAsyncImage(
                                        model = state.podcast.imageUrl,
                                        contentDescription = state.podcast.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = { AnimatedShapesFallback() },
                                        error = { AnimatedShapesFallback() }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // 2. Metadata Column (Right)
                                Column(modifier = Modifier.weight(1f)) {
                                    // Artist
                                    Text(
                                        text = state.podcast.artist,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Description (Expandable)
                                    val strippedDesc = stripHtml(state.podcast.description)
                                    var isDescExpanded by remember { mutableStateOf(false) }
                                    
                                    if (strippedDesc.isNotEmpty()) {
                                        Text(
                                            text = strippedDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 16.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .expressiveClickable { isDescExpanded = !isDescExpanded }
                                                .animateContentSize(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    
                                    // Genre Pill
                                    if (state.podcast.genre.isNotEmpty()) {
                                        Surface(
                                            shape = ExpressiveShapes.Pill,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text = state.podcast.genre,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Old Description Removed (Integrated into Hero Row)
                    
                    // EPISODE TOOLBAR (M3 Expressive)
                    item(key = "toolbar") {
                        EpisodeToolbar(
                            searchQuery = state.searchQuery,
                            onSearchChange = { viewModel.searchEpisodes(it) },
                            isSearching = state.isSearching,
                            currentSort = state.currentSort,
                            onSortToggle = { viewModel.toggleSort() },
                            isSubscribed = state.isSubscribed,
                            onSubscribeClick = { viewModel.toggleSubscription() },
                            accentColor = accentColor,
                            onSearchFocused = {
                                // Scroll to show toolbar at top of visible area
                                coroutineScope.launch {
                                    // Scroll to hero (item 0) with large offset so toolbar appears at top
                                    listState.animateScrollToItem(index = 0, scrollOffset = 500)
                                }
                            }
                        )
                    }
                    
                    // Episodes (Use search results if searching, else main list)
                    val displayEpisodes = state.searchResults ?: state.episodes
                    
                    itemsIndexed(displayEpisodes, key = { _, ep -> ep.id }) { index, episode ->
                         EpisodeListItem(
                            episode = episode,
                            accentColor = accentColor,
                            onClick = { onEpisodeClick(episode) },
                            onPlayClick = { onPlayEpisode(episode) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Infinite scroll trigger (when not searching)
                        if (state.searchResults == null && index == displayEpisodes.lastIndex && state.hasMoreEpisodes && !state.isLoadingMore) {
                            LaunchedEffect(displayEpisodes.size) {
                                viewModel.loadMoreEpisodes()
                            }
                        }
                    }
                    
                    // Loading indicator for pagination
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BoxCastLoader.CircularWavy(size = 32.dp)
                            }
                        }
                    }
                    
                    // Empty search results message
                    if (state.searchResults?.isEmpty() == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No episodes found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // FIXED HEADER (like Episode Info)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(collapsedHeaderHeight)
                        .background(headerColor)
                        .statusBarsPadding()
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // FLOATING TITLE - physically moves from body to header
                Text(
                    text = state.podcast.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = titleHorizontalPadding)
                        .graphicsLayer { 
                            translationY = titleTranslationY
                            alpha = titleAlpha 
                        }
                )
            }
        }
    }
}

@Composable
fun EpisodeListItem(
    episode: Episode,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.medium, // Regular rounded
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                 SubcomposeAsyncImage(
                    model = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { AnimatedShapesFallback() },
                    error = { AnimatedShapesFallback() }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Duration and Release Date
                fun formatDuration(seconds: Int): String {
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                }
                
                fun formatRelativeDate(timestampSeconds: Long): String {
                    if (timestampSeconds == 0L) return ""
                    val now = System.currentTimeMillis() / 1000
                    val diff = now - timestampSeconds
                    return when {
                        diff < 3600 -> "${diff / 60}m ago"
                        diff < 86400 -> "${diff / 3600}h ago"
                        diff < 604800 -> "${diff / 86400}d ago"
                        diff < 2592000 -> "${diff / 604800}w ago"
                        diff < 31536000 -> "${diff / 2592000}mo ago"
                        else -> "${diff / 31536000}y ago"
                    }
                }
                
                val relativeDate = formatRelativeDate(episode.publishedDate)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(episode.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (relativeDate.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = relativeDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            FilledIconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor
                ),
                modifier = Modifier.expressiveClickable(onClick = onPlayClick)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Episode Toolbar - M3 Expressive
 * Contains: Search, Sort Toggle, Subscribe Button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isSearching: Boolean,
    currentSort: EpisodeSort,
    onSortToggle: () -> Unit,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
    accentColor: Color,
    onSearchFocused: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Field
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    color = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                    shape = ExpressiveShapes.Pill
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = if (isFocused) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        onSearchFocused()
                                    }
                                },
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty() && !isFocused) {
                                        Text(
                                            text = "Search episodes...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    onSearchChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (isSearching) {
                            BoxCastLoader.Expressive(size = 20.dp)
                        }
                    }
                }
            }
            
            // Controls Row: Sort + Subscribe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Chip
                val sortInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isSortPressed by sortInteractionSource.collectIsPressedAsState()
                val sortScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSortPressed) 0.9f else 1f,
                    animationSpec = if (isSortPressed) cx.aswin.boxcast.core.designsystem.theme.ExpressiveMotion.QuickSpring else cx.aswin.boxcast.core.designsystem.theme.ExpressiveMotion.BouncySpring,
                    label = "sortScale"
                )
                
                FilterChip(
                    selected = true,
                    onClick = onSortToggle,
                    label = { 
                        Text(
                            text = if (currentSort == EpisodeSort.NEWEST) "Newest" else "Oldest",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (currentSort == EpisodeSort.NEWEST) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    interactionSource = sortInteractionSource,
                    modifier = Modifier.graphicsLayer { 
                        scaleX = sortScale
                        scaleY = sortScale
                    }
                )
                
                // Subscribe Button
                val subInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isSubPressed by subInteractionSource.collectIsPressedAsState()
                val subScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSubPressed) 0.9f else 1f,
                    animationSpec = if (isSubPressed) cx.aswin.boxcast.core.designsystem.theme.ExpressiveMotion.QuickSpring else cx.aswin.boxcast.core.designsystem.theme.ExpressiveMotion.BouncySpring,
                    label = "subScale"
                )

                FilledTonalButton(
                    onClick = onSubscribeClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSubscribed) accentColor.copy(alpha = 0.15f) else accentColor,
                        contentColor = if (isSubscribed) accentColor else Color.White
                    ),
                    interactionSource = subInteractionSource,
                    modifier = Modifier.graphicsLayer { 
                        scaleX = subScale
                        scaleY = subScale
                    }
                ) {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
