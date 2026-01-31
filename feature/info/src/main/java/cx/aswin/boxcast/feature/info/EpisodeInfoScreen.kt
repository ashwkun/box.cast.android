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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
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
import cx.aswin.boxcast.core.designsystem.component.HtmlText
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

    // Morphing Header State
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 2000f // Fully collapsed
        }
    }
    val density = LocalDensity.current
    val morphThreshold = with(density) { 200.dp.toPx() } // Density-aware threshold
    val morphFraction = (scrollOffset / morphThreshold).coerceIn(0f, 1f)
    
    // Interpolated Values
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // Height: Compact expanded state (140dp + SB) -> Standard toolbar (64dp + SB)
    // Increased to 200dp to accommodate longer titles (3-4 lines) without truncation
    val expandedHeight = 200.dp + statusBarHeight
    val collapsedHeight = 64.dp + statusBarHeight
    
    // Polished animation with M3 Expressive easing
    val headerHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(expandedHeight, collapsedHeight, morphFraction),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
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
    
    // Bottom Padding Interpolation: 16dp (Expanded) -> 20dp (Collapsed - Vertically centered)
    val titleBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(16.dp, 20.dp, morphFraction),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "titleBottomPadding"
    )

    when (val state = uiState) {
        is EpisodeInfoUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BoxCastLoader.Expressive(size = 80.dp)
            }
        }
        is EpisodeInfoUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load episode", color = MaterialTheme.colorScheme.error)
            }
        }
        is EpisodeInfoUiState.Success -> {
            // Color extraction
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(state.episode.podcastImageUrl?.ifEmpty { state.episode.imageUrl?.ifEmpty { null } })
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
            
            Box(modifier = modifier.fillMaxSize()) {
                // Content List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = expandedHeight + 16.dp, // Start content BELOW the expanded header
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + bottomContentPadding + 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Center everything
                ) {
                    // HERO SECTION
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // No Spacer needed for Back Button as Header handles it
                            // No Body Title - moved to Header Overlay
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Artwork (Centered & Large)
                            Surface(
                                modifier = Modifier.size(200.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                shadowElevation = 12.dp
                            ) {
                                AsyncImage(
                                    model = state.episode.imageUrl?.ifEmpty { null },
                                    contentDescription = state.episode.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Podcast Title
                            Text(
                                text = state.podcastTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.expressiveClickable { onPodcastClick(state.podcastId) }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Metadata Row
                            val durationText = if (episodeDuration > 3600) 
                                "${episodeDuration / 3600}hr ${(episodeDuration % 3600) / 60}min" 
                            else "${(episodeDuration % 3600) / 60} min"
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Audio", // Placeholder type
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // ACTION ROW
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Main Play Button
                            FilledTonalButton(
                                onClick = {}, // Handled by expressiveClickable for bounce effect
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .expressiveClickable(onClick = onPlay),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.resumePositionMs > 0) "Resume" else "Play",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (state.resumePositionMs > 0 && state.durationMs > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val progress = (state.resumePositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                LinearWavyProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // DESCRIPTION SECTION
                    if (state.episode.description.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = "About this episode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Box(modifier = Modifier.padding(16.dp)) {
                                        HtmlText(
                                            text = state.episode.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // MORPHING HEADER OVERLAY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(headerColor)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.BottomCenter // Centered Alignment
                ) {
                    // Back Button
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Morphing Title
                    // Always visible, interpolating size
                    
                    // Interpolated Text Size
                    val startSize = MaterialTheme.typography.headlineSmall.fontSize // Start Large
                    val endSize = MaterialTheme.typography.titleLarge.fontSize // End Small (Match Podcast Screen)
                    val currentSize = androidx.compose.ui.unit.lerp(startSize, endSize, morphFraction)
                    
                    // Smooth transition: Fade alpha during maxLines change
                    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (morphFraction < 0.7f) 1f else 0.95f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "textAlpha"
                    )
                    
                    Text(
                        text = episodeTitle,
                        fontSize = currentSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (morphFraction > 0.5f) 1 else 4, // Allow up to 4 lines when expanded
                        minLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .padding(start = 48.dp, end = 48.dp, bottom = titleBottomPadding) // Symmetric padding
                            .fillMaxWidth()
                            .graphicsLayer { alpha = textAlpha }
                    )
                }
            }
        }
    }
}
