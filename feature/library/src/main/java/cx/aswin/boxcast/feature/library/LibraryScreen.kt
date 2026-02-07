package cx.aswin.boxcast.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast

/**
 * Main Library Screen
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToLiked: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        uiState = uiState,
        onNavigateToLiked = onNavigateToLiked,
        onNavigateToSubscriptions = onNavigateToSubscriptions,
        onNavigateToDownloads = onNavigateToDownloads
    )
}

@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onNavigateToLiked: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    // Background gradient for subtle depth
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // === MAIN LIBRARY PAGE ===
            Text(
                text = "Library",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)
            )

            // CONTENT
            when (uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LibraryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error loading library")
                    }
                }
                is LibraryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        item {
                            val subscriptionImages = (uiState as LibraryUiState.Success).subscribedPodcasts.take(3).map { it.imageUrl }
                            val subShapes = listOf(
                                ExpressiveShapes.Circle,
                                ExpressiveShapes.Puffy,
                                ExpressiveShapes.Diamond // Fallback to Circle/Diamond if Squircle missing or use Diamond
                            ).map { if (it == ExpressiveShapes.Circle) ExpressiveShapes.Circle else it } // Just logic placeholder
                            // Using: Circle, Puffy, Diamond (as Squircle replacement)
                            val specificSubShapes = listOf(
                                ExpressiveShapes.Circle,
                                ExpressiveShapes.Puffy,
                                ExpressiveShapes.Diamond
                            )
                            
                            LibraryMenuCard(
                                title = "Subscriptions",
                                icon = Icons.Rounded.Add,
                                onClick = onNavigateToSubscriptions,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                images = subscriptionImages,
                                shapes = specificSubShapes
                            )
                        }
                        item {
                            val likedImages = (uiState as LibraryUiState.Success).likedEpisodes.take(3).map { it.episodeImageUrl ?: it.podcastImageUrl ?: "" }
                            val likedShapes = listOf(
                                ExpressiveShapes.Heart,
                                ExpressiveShapes.Star,
                                ExpressiveShapes.SoftBurst
                            )
                            
                            LibraryMenuCard(
                                title = "Liked Episodes",
                                icon = Icons.Rounded.Favorite,
                                onClick = onNavigateToLiked,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                images = likedImages,
                                shapes = likedShapes
                            )
                        }

                        item {
                            val downloadImages = (uiState as LibraryUiState.Success).downloadedEpisodes.take(3).map { it.episodeImageUrl ?: it.podcastImageUrl ?: "" }
                            val downloadShapes = listOf(
                                ExpressiveShapes.Hexagon,
                                ExpressiveShapes.Gem,
                                ExpressiveShapes.Cookie4
                            )
                            
                            LibraryMenuCard(
                                title = "Downloads",
                                icon = Icons.Rounded.DownloadDone,
                                onClick = onNavigateToDownloads,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                images = downloadImages,
                                shapes = downloadShapes
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryMenuCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    images: List<String> = emptyList(),
    shapes: List<androidx.compose.ui.graphics.Shape> = emptyList()
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .expressiveClickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Collage Background (Right Side)
            if (images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f) // Take up half the card width
                ) {
                    LibraryCardCollage(images = images, shapes = shapes)
                }
                
                // Scrim to ensure text readability if images slide under text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    containerColor,
                                    containerColor.copy(alpha = 0.8f),
                                    containerColor.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
            )
        }
    }
}

@Composable
private fun LibraryCardCollage(
    images: List<String>, 
    shapes: List<androidx.compose.ui.graphics.Shape>
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Fallback shapes if list is empty or short
        val fallbackShapes = listOf(
            ExpressiveShapes.SoftBurst,
            ExpressiveShapes.Circle,
            ExpressiveShapes.Diamond
        )
        val finalShapes = if (shapes.isNotEmpty()) shapes else fallbackShapes
        
        // Reverse order so first image is on top
        images.take(3).reversed().forEachIndexed { index, imageUrl ->
            // Calculate reverse index for correct shape assignment (0=Top, etc)
            val realIndex = images.size - 1 - index
            val shape = finalShapes.getOrElse(realIndex) { finalShapes.first() }
            
            // Dynamic offsets for "pile" effect
            val xOffset = (realIndex * 20).dp
            val yOffset = if (realIndex % 2 == 0) 10.dp else (-10).dp
            val scale = 1f - (realIndex * 0.15f)
            val rotation = if (realIndex % 2 == 0) 10f else -10f

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = xOffset, y = yOffset)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    .clip(shape)
                    .border(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape)
            )
        }
    }
}

@Composable
fun ExpressiveSolarSystemEmptyState(onExploreClick: () -> Unit) {
    // Randomize shapes on composition
    val shapes = remember {
        listOf(
            ExpressiveShapes.Burst, ExpressiveShapes.SoftBurst, ExpressiveShapes.Boom, 
            ExpressiveShapes.SoftBoom, ExpressiveShapes.Star, ExpressiveShapes.Sunny, 
            ExpressiveShapes.VerySunny, ExpressiveShapes.Flower, ExpressiveShapes.Puffy,
            ExpressiveShapes.Clover4, ExpressiveShapes.Clover8, ExpressiveShapes.Heart,
            ExpressiveShapes.Diamond, ExpressiveShapes.Gem, ExpressiveShapes.Hexagon
        ).shuffled().take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Solar System Composition (Static & Expressive)
        Box(
            modifier = Modifier.size(280.dp), // Large canvas
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Background Shape (Random 1)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = shapes[0]
                    )
            )
            
            // Layer 2: Floating Shape (Random 2)
            Box(
                 modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-40).dp, y = 40.dp)
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = shapes[1]
                    )
            )
            
            // Layer 3: Foreground Shape (Random 3)
            Box(
                 modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 40.dp, y = (-50).dp)
                    .size(width = 70.dp, height = 90.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = shapes[2]
                    )
            )
            
            // Center Anchor Icon
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "It's a big universe out there.\nStart exploring.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onExploreClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .height(56.dp)
                .expressiveClickable(onClick = onExploreClick)
        ) {
            Text(
                "Go Explore", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

/**
 * Unified Card Style matching ExploreScreen.kt
 * Uses OutlinedCard, specific rounded corners, variable heights.
 */
@Composable
fun LibraryPodcastCard(
    podcast: Podcast,
    isTall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.large, // Matches ExploreScreen
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onClick)
    ) {
        Column {
            // Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTall) 200.dp else 150.dp) // Staggered heights (Reduced)
            ) {
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) // Matches Explore styling
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Text Content with Padding
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
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
