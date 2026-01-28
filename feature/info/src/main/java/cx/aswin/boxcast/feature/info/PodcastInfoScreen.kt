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
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Episode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    // Dynamic color
    var extractedColor by remember { mutableStateOf(Color.Transparent) }
    val accentColor by animateColorAsState(
        targetValue = if (extractedColor != Color.Transparent) extractedColor else MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "accentColor"
    )

    LaunchedEffect(podcastId) {
        viewModel.loadPodcast(podcastId)
    }
    
    // Scroll state for morphing header
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 2000f // Fully collapsed
        }
    }
    
    // Morph Fraction: 0f (Expanded) -> 1f (Collapsed)
    // Threshold: 300px
    val morphThreshold = 300f
    val morphFraction = (scrollOffset / morphThreshold).coerceIn(0f, 1f)
    
    // Interpolated Values
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // Height: Compact expanded state (140dp + SB) -> Standard toolbar (64dp + SB)
    val expandedHeight = 140.dp + statusBarHeight
    val collapsedHeight = 64.dp + statusBarHeight
    
    // Polished animation with M3 Expressive easing
    val headerHeight by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(expandedHeight, collapsedHeight, morphFraction),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headerHeight"
    )
    val headerColor by animateColorAsState(
        targetValue = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface, 
            MaterialTheme.colorScheme.surfaceContainer, 
            morphFraction
        ),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "headerColor"
    )
    
    // Left Padding Interpolation: 20dp (Expanded) -> 56dp (Collapsed, standard keyline)
    val titleStartPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(20.dp, 56.dp, morphFraction),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "titleStartPadding"
    )
    
    // Bottom Padding Interpolation: 16dp (Expanded) -> 20dp (Collapsed - Vertically centered)
    val titleBottomPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(16.dp, 20.dp, morphFraction),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "titleBottomPadding"
    )
    
    // Back button alpha (fade in as we scroll for better contrast)
    // Removed: backButtonBgAlpha - no longer using container

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
                // Color extraction
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(state.podcast.imageUrl)
                        .size(Size(100, 100))
                        .allowHardware(false)
                        .build()
                )
                LaunchedEffect(painter.state) {
                    val painterState = painter.state
                    if (painterState is AsyncImagePainter.State.Success) {
                        val bitmap = (painterState.result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            extractedColor = extractDominantColor(bitmap)
                        }
                    }
                }
                
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
                        top = 140.dp + statusBarHeight + 16.dp, // Match expandedHeight + padding
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO SECTION: Compact Row (Image + Metadata)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically // Top align might be better if desc is long, but Center is safe for scale
                        ) {
                            
                            // 1. Small Shaped Image (Left)
                            ElevatedCard(
                                modifier = Modifier.size(100.dp), // Efficient tiny size
                                shape = MaterialTheme.shapes.large, // Squircle
                                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                            ) {
                                AsyncImage(
                                    model = state.podcast.imageUrl,
                                    contentDescription = state.podcast.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // 2. Metadata Column (Right)
                            Column(modifier = Modifier.weight(1f)) {
                                // Arist
                                Text(
                                    text = state.podcast.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                // Description (Integrated & Expandable)
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
                                // Scroll toolbar to very top when search is focused
                                coroutineScope.launch {
                                    // Scroll to item 1 (toolbar) with offset to push it to top of screen
                                    listState.animateScrollToItem(index = 1, scrollOffset = -200)
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
                
                // TOP MORPHING HEADER
                // Replaces ToolBar. Grows and shrinks.
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(headerColor)
                        .statusBarsPadding(), // Ensures it respects system bars
                    contentAlignment = Alignment.BottomStart // Align text to Bottom Left
                ) {
                    // Back Button (Top Left) - Clean, no container
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.expressiveClickable(onClick = onBack)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Title and Artist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = titleBottomPadding, start = titleStartPadding, end = 16.dp), // Interpolated padding
                        horizontalAlignment = Alignment.Start // Left Align
                    ) {
                        // Interpolated Text Size
                        // Use Larger Sizes as requested
                        val startSize = MaterialTheme.typography.headlineSmall.fontSize // 24sp
                        val endSize = MaterialTheme.typography.titleLarge.fontSize // 22sp
                        val currentSize = androidx.compose.ui.unit.lerp(startSize, endSize, morphFraction)
                        
                        // Smooth transition: Always show 2 lines but fade overflow
                        // Instead of abrupt maxLines change, use alpha crossfade
                        val textAlpha by animateFloatAsState(
                            targetValue = if (morphFraction < 0.7f) 1f else 0.95f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "textAlpha"
                        )
                        
                        Text(
                            text = state.podcast.title,
                            fontSize = currentSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (morphFraction > 0.5f) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start // Left Align
                        )
                    }
                }
                

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
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
                // Sort Chip (no expressiveClickable - built-in ripple)
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
                    )
                )
                
                // Subscribe Button (no expressiveClickable - built-in ripple)
                FilledTonalButton(
                    onClick = onSubscribeClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSubscribed) accentColor.copy(alpha = 0.15f) else accentColor,
                        contentColor = if (isSubscribed) accentColor else Color.White
                    )
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
