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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    
    // Height: Compact expanded state (120dp + SB) -> Standard toolbar (64dp + SB)
    // We reduce expanded height to minimize gap, as title is mostly 1-2 lines.
    val expandedHeight = 120.dp + statusBarHeight
    val collapsedHeight = 64.dp + statusBarHeight
    
    val headerHeight by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(expandedHeight, collapsedHeight, morphFraction),
        label = "headerHeight"
    )
    val headerColor by animateColorAsState(
        targetValue = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceContainer, morphFraction),
        label = "headerColor"
    )
    
    // Left Padding Interpolation: 20dp (Expanded) -> 64dp (Collapsed, standard keyline)
    val titleStartPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(20.dp, 64.dp, morphFraction),
        label = "titleStartPadding"
    )
    
    // Bottom Padding Interpolation: 12dp (Expanded) -> 18.dp (Collapsed - Vertically centered in 64dp)
    val titleBottomPadding by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(12.dp, 18.dp, morphFraction),
        label = "titleBottomPadding"
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 120.dp + statusBarHeight + 16.dp, // Match expandedHeight + padding
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 80.dp
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
                                            .clickable { isDescExpanded = !isDescExpanded }
                                            .animateContentSize()
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
                    
                    // Episodes Header
                    item {
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Episodes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = accentColor.copy(alpha = 0.15f),
                                        shape = ExpressiveShapes.Cookie6
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${state.episodes.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Episodes
                    itemsIndexed(state.episodes, key = { _, ep -> ep.id }) { index, episode ->
                         EpisodeListItem(
                            episode = episode,
                            accentColor = accentColor,
                            onClick = { onEpisodeClick(episode) },
                            onPlayClick = { onPlayEpisode(episode) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
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
                    // Back Button (Top Left)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
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
                        val endSize = MaterialTheme.typography.titleLarge.fontSize // 22sp (Larger than Medium)
                        val currentSize = androidx.compose.ui.unit.lerp(startSize, endSize, morphFraction)
                        
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
                
                // FAB
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 16.dp, 
                            end = 16.dp
                        )
                ) {
                    ExpressiveExtendedFab(
                        text = if (state.isSubscribed) "Subscribed" else "Subscribe",
                        icon = if (state.isSubscribed) Icons.Rounded.Check else Icons.Rounded.Add,
                        onClick = { viewModel.toggleSubscription() }
                    )
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
                fun formatDuration(seconds: Int): String {
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                }
                Text(
                    text = formatDuration(episode.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
