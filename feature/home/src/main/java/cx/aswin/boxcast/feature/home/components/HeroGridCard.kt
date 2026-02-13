package cx.aswin.boxcast.feature.home.components

import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import cx.aswin.boxcast.core.model.Podcast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import cx.aswin.boxcast.core.designsystem.components.AnimatedShapesFallback


@Composable
fun HeroGridCard(
    items: List<Podcast>,
    title: String,
    onPlayClick: (Podcast) -> Unit,
    onDetailsClick: (Podcast) -> Unit, // Renamed from onClick for clarity
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Expressive Shapes Background
            ExpressiveBackground()
            
            // DEBUG LOGGING
            androidx.compose.runtime.LaunchedEffect(items) {
                items.take(6).forEachIndexed { index, podcast ->
                    android.util.Log.d("HeroGrid", "Update [$index]: Pod=${podcast.title}, Ep=${podcast.latestEpisode?.title}, Auth=${podcast.artist}, Img=${podcast.imageUrl}")
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                RowHeader(title = title)
    
                // Dynamic Bento Grid Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                // Ensure we handle cases safely
                val displayItems = items.take(6)
                
                when (displayItems.size) {
                    2 -> GridLayout2(displayItems, onPlayClick, onDetailsClick)
                    3 -> GridLayout3(displayItems, onPlayClick, onDetailsClick)
                    4 -> GridLayout4(displayItems, onPlayClick, onDetailsClick)
                    5 -> GridLayout5(displayItems, onPlayClick, onDetailsClick)
                    6 -> GridLayout6(displayItems, onPlayClick, onDetailsClick)
                    else -> if (displayItems.isNotEmpty()) GridLayoutGeneric(displayItems, onPlayClick, onDetailsClick)
                }
            }
        }
    }
}
}

// --- Dynamic Layouts ---

@Composable
private fun GridLayout2(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // 2 Vertical Splits (Tall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        GridItem(items[0], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f).fillMaxHeight())
        GridItem(items[1], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun GridLayout3(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // 1 Big Left, 2 Small Right (Stacked)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        GridItem(items[0], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f).fillMaxHeight())
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
            GridItem(items[1], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f).fillMaxWidth())
            GridItem(items[2], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun GridLayout4(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // 2x2 Grid
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[0], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[1], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[2], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[3], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GridLayout5(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // Top: 2 items, Bottom: 3 items (Squats)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[0], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[1], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[2], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[3], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[4], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GridLayout6(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // 3x2 Grid (Original)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[0], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[1], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[2], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            GridItem(items[3], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[4], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
            GridItem(items[5], onPlayClick, onDetailsClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GridLayoutGeneric(items: List<Podcast>, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit) {
    // Fallback? Should not happen often given logical constraints
    GridLayout2(items.take(2), onPlayClick, onDetailsClick)
}

// --- Shared Components ---

@Composable
private fun RowHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.lowercase().replaceFirstChar { it.uppercase() }, // Title Case
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GridItem(podcast: Podcast, onPlayClick: (Podcast) -> Unit, onDetailsClick: (Podcast) -> Unit, modifier: Modifier = Modifier) {
    // Use rememberUpdatedState to ensure we always use the latest podcast data
    val currentPodcast by androidx.compose.runtime.rememberUpdatedState(podcast)
    val currentOnPlayClick by androidx.compose.runtime.rememberUpdatedState(onPlayClick)
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.dp, // Flat inside card looks cleaner
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .expressiveClickable { 
                android.util.Log.d("HeroGrid", "GridItem clicked: ${currentPodcast.title} (id=${currentPodcast.id})")
                currentOnPlayClick(currentPodcast) 
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image Logic: Episode Art -> Podcast Art -> Shapes
            var currentModel by remember(podcast.imageUrl) { mutableStateOf(podcast.imageUrl.ifEmpty { null }) }

            SubcomposeAsyncImage(
                model = currentModel,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                     if (state is AsyncImagePainter.State.Error) {
                         if (currentModel == podcast.imageUrl && !podcast.fallbackImageUrl.isNullOrEmpty()) {
                             currentModel = podcast.fallbackImageUrl
                         }
                     }
                }
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error || currentModel == null) {
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
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Text Label
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
            )
            

            
            // Expressive Progress Bar
            if (podcast.resumeProgress != null) {
                ExpressiveProgressBar(
                    progress = podcast.resumeProgress!!,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.4f),
    indicatorColor: Color = MaterialTheme.colorScheme.inversePrimary // High contrast on dark gradient
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(indicatorColor)
        )
    }
}

@Composable
private fun ExpressiveBackground() {
    val shapes = androidx.compose.runtime.remember {
        val allShapes = listOf(
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Sunny, 
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Flower, 
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Boom, 
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Puffy,
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Heart,
            cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes.Diamond
        ).shuffled()
        
        listOf(
            Triple(0.2f, 0.2f, allShapes[0]),
            Triple(0.8f, 0.3f, allShapes[1]),
            Triple(0.1f, 0.8f, allShapes[2]),
            Triple(0.9f, 0.7f, allShapes[3])
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)
    ) {
        shapes.forEach { (relX, relY, shape) ->
            // Large background shapes
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopStart) // Base alignment
                    .offset(x = (300 * relX).dp, y = (500 * relY).dp) // Approximate offset
                    .offset(x = (-100).dp, y = (-100).dp) // Center the 200dp box
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = shape
                    )
            )
        }
    }
}


