package cx.aswin.boxcast.feature.info

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable

// Color extraction helper
private fun extractDominantColor(bitmap: android.graphics.Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val vibrant = palette.vibrantSwatch?.rgb
    val muted = palette.mutedSwatch?.rgb
    val dominant = palette.dominantSwatch?.rgb
    val colorInt = vibrant ?: muted ?: dominant ?: 0xFF6200EE.toInt()
    return Color(colorInt)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EpisodeInfoScreen(
    episodeId: String,
    episodeTitle: String,
    episodeDescription: String,
    episodeImageUrl: String,
    episodeAudioUrl: String,
    episodeDuration: Int,
    podcastId: String,
    podcastTitle: String,
    viewModel: EpisodeInfoViewModel,
    onBack: () -> Unit,
    onPodcastClick: (String) -> Unit,
    onPlay: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    // Dynamic color extraction
    var extractedColor by remember { mutableStateOf(Color.Transparent) }
    val accentColor by animateColorAsState(
        targetValue = if (extractedColor != Color.Transparent) extractedColor else MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "accent_color"
    )

    LaunchedEffect(episodeId) {
        viewModel.loadEpisode(
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            episodeDescription = episodeDescription,
            episodeImageUrl = episodeImageUrl,
            episodeAudioUrl = episodeAudioUrl,
            episodeDuration = episodeDuration,
            podcastId = podcastId,
            podcastTitle = podcastTitle
        )
    }

    // Morphing header calculations
    val collapsedHeight = 64.dp
    val expandedHeight = 140.dp
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    val scrollOffset by remember {
        derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() }
    }
    val firstVisibleItem by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    
    val headerProgress by remember {
        derivedStateOf {
            if (firstVisibleItem > 0) 1f
            else (scrollOffset / with(density) { 200.dp.toPx() }).coerceIn(0f, 1f)
        }
    }

    when (val state = uiState) {
        is EpisodeInfoUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BoxCastLoader.Expressive(size = 64.dp)
            }
        }
        is EpisodeInfoUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load episode", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        is EpisodeInfoUiState.Success -> {
            // Image painter for color extraction
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(state.episode.imageUrl?.ifEmpty { null })
                    .allowHardware(false)
                    .build()
            )
            val painterState = painter.state
            
            LaunchedEffect(painterState) {
                if (painterState is AsyncImagePainter.State.Success) {
                    val bitmap = (painterState.result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        extractedColor = extractDominantColor(bitmap)
                    }
                }
            }
            
            Box(modifier = modifier.fillMaxSize()) {
                // Content
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = expandedHeight + statusBarHeight + 16.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO SECTION: Episode Image + Metadata
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Episode Artwork
                            Surface(
                                modifier = Modifier.size(140.dp),
                                shape = ExpressiveShapes.Cookie4,
                                shadowElevation = 8.dp
                            ) {
                                AsyncImage(
                                    model = state.episode.imageUrl?.ifEmpty { null },
                                    contentDescription = state.episode.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Episode Metadata
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Duration
                                val hours = episodeDuration / 3600
                                val minutes = (episodeDuration % 3600) / 60
                                val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                                
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = ExpressiveShapes.Pill
                                ) {
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                                
                                // Resume Progress
                                if (state.resumePositionMs > 0 && state.durationMs > 0) {
                                    val progress = (state.resumePositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                                    val remaining = ((state.durationMs - state.resumePositionMs) / 60000).coerceAtLeast(1)
                                    
                                    Column {
                                        Text(
                                            text = "${remaining}m remaining",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearWavyProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = accentColor,
                                            trackColor = accentColor.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                                
                                // Podcast Link
                                FilledTonalButton(
                                    onClick = { onPodcastClick(state.podcastId) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Podcasts,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = state.podcastTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Play Button
                                FilledTonalButton(
                                    onClick = onPlay,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.resumePositionMs > 0) "Resume" else "Play",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // DESCRIPTION
                    if (state.episode.description.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "About this episode",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.episode.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // MORPHING HEADER
                val headerHeight = collapsedHeight + (expandedHeight - collapsedHeight) * (1f - headerProgress)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight + statusBarHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f * (1f - headerProgress)),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = statusBarHeight + 8.dp, start = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Title (morphs on scroll)
                    val titleAlpha = headerProgress
                    if (titleAlpha > 0.3f) {
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 56.dp, end = 16.dp)
                                .graphicsLayer { alpha = titleAlpha }
                        )
                    }
                }
            }
        }
    }
}
