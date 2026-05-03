package cx.aswin.boxcast.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter

// Sorted: All first, then by installed user audience sort
private val countryOptions = listOf(
    "ALL" to "All",
    "US" to "United States",
    "IN" to "India",
    "GB" to "United Kingdom",
    "PH" to "Philippines",
    "RU" to "Russia",
    "MA" to "Morocco",
    "ET" to "Ethiopia",
    "AF" to "Afghanistan",
    "KE" to "Kenya",
    "JP" to "Japan",
    "BR" to "Brazil",
    "ID" to "Indonesia",
    "DE" to "Germany",
    "FR" to "France",
    "AU" to "Australia",
    "CA" to "Canada"
)

// Height variations for the staggered Pinterest effect
private enum class CardSize(val heightDp: Int) {
    TALL(220),
    MEDIUM(180),
    SHORT(150)
}

// Deterministic pattern so it doesn't shuffle on recomposition
private fun getCardSize(index: Int): CardSize {
    return when (index % 6) {
        0 -> CardSize.TALL
        1 -> CardSize.SHORT
        2 -> CardSize.MEDIUM
        3 -> CardSize.SHORT
        4 -> CardSize.TALL
        5 -> CardSize.MEDIUM
        else -> CardSize.MEDIUM
    }
}

// --- Main Feed ---

@Composable
fun RadioFeed(
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    podcastRepository: cx.aswin.boxcast.core.data.PodcastRepository,
    playingStationId: String? = null,
    isRadioPlaying: Boolean = false,
    onPlayStation: (RadioStation) -> Unit = { _ -> },
    followedStationIds: Set<String> = emptySet(),
    onToggleFollow: (RadioStation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var allStations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Helper to map API items to UI model
    fun mapStation(item: cx.aswin.boxcast.core.network.model.RadioStationItem): RadioStation {
        val colorInt = try {
            if (item.image.isNotEmpty()) {
                val hash = item.image.hashCode()
                val r = (hash and 0xFF0000) shr 16
                val g = (hash and 0x00FF00) shr 8
                val b = (hash and 0x0000FF)
                Color(r / 2, g / 2, b / 2)
            } else {
                Color(0xFF2A3A4A)
            }
        } catch(e: Exception) { Color(0xFF2A3A4A) }
        
        return RadioStation(
            id = item.id,
            name = item.title.trim(),
            genre = item.tags
                .filter { it.lowercase() !in listOf("radio", "music", "fm") && it.isNotBlank() }
                .firstOrNull()
                ?.replaceFirstChar { it.uppercase() }
                ?: item.language.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
                ?: "Radio",
            tags = item.tags.filter { it.lowercase() != "radio" && it.isNotBlank() },
            frequency = "Online",
            location = item.country.ifEmpty { "Global" },
            accentColor = colorInt,
            imageUrl = item.image,
            streamUrl = item.streamUrl,
            country = item.country,
            language = item.language,
            bitrate = item.bitrate,
            codec = item.codec,
            votes = item.votes
        )
    }

    // Fetch user's country
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val cfResult = podcastRepository.getRadioLocate()
            val detectedCountry = cfResult?.country ?: "US"
            selectedCountry = if (countryOptions.any { it.first == detectedCountry }) {
                detectedCountry
            } else {
                "ALL"
            }
        } catch (e: Exception) {
            selectedCountry = "ALL"
        }
    }

    // Fetch stations when country changes
    androidx.compose.runtime.LaunchedEffect(selectedCountry) {
        if (selectedCountry == null) return@LaunchedEffect
        isLoading = true
        try {
            val qCountry = if (selectedCountry == "ALL") null else selectedCountry
            val popularData = podcastRepository.getPopularRadioStations(country = qCountry, limit = 50)
            if (popularData.isNotEmpty()) {
                allStations = popularData.map { mapStation(it) }
            }
        } catch(e: Exception) {
            if (e is java.util.concurrent.CancellationException || e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("RadioFeed", "Failed to load radio data", e)
        } finally {
            isLoading = false
        }
    }

    LazyVerticalStaggeredGrid(
        state = gridState,
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 160.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        // Country chip selector — full width
        item(span = StaggeredGridItemSpan.FullLine) {
            CountryLanguageBar(
                selectedCountry = selectedCountry ?: "",
                onCountrySelected = { selectedCountry = it }
            )
        }

        // Pinterest-style station cards
        items(
            items = allStations,
            key = { it.id }
        ) { station ->
            val index = allStations.indexOf(station)
            val cardSize = getCardSize(index)
            
            PinterestStationCard(
                station = station,
                isPlaying = playingStationId == station.id && isRadioPlaying,
                isFollowed = station.id in followedStationIds,
                onClick = { onPlayStation(station) },
                onFollowClick = { onToggleFollow(station) },
                heightDp = cardSize.heightDp,
                modifier = Modifier
            )
        }
    }
}

// --- Country Chip Scroller ---

@Composable
private fun CountryLanguageBar(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayOptions = remember(selectedCountry) {
        val allOption = countryOptions.find { it.first == "ALL" } ?: ("ALL" to "All")
        val selectedOption = countryOptions.find { it.first == selectedCountry && it.first != "ALL" }
        val remainingOptions = countryOptions.filter { it.first != "ALL" && it.first != selectedCountry }
        
        buildList {
            add(allOption)
            if (selectedOption != null) add(selectedOption)
            addAll(remainingOptions)
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(displayOptions) { (code, label) ->
            val isSelected = selectedCountry == code
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable { onCountrySelected(code) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// --- Pinterest Station Card ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PinterestStationCard(
    station: RadioStation,
    isPlaying: Boolean = false,
    isFollowed: Boolean = false,
    onClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    heightDp: Int = 180,
    modifier: Modifier = Modifier
) {
    // Follow animation state
    var showFollowLabel by remember { mutableStateOf(false) }
    var followLabelText by remember { mutableStateOf("") }
    
    // Auto-dismiss the label after a delay
    androidx.compose.runtime.LaunchedEffect(showFollowLabel) {
        if (showFollowLabel) {
            kotlinx.coroutines.delay(800)
            showFollowLabel = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Image area with overlaid action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Artwork or gradient fallback
                if (station.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = station.imageUrl,
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Error,
                            is AsyncImagePainter.State.Loading -> {
                                GradientFallback(station)
                            }
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                } else {
                    GradientFallback(station)
                }

                // Now Playing overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerBars()
                    }
                }

                // Play + Follow buttons — bottom-right of image
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Follow button — animated
                    Box(
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                followLabelText = if (isFollowed) "Unfollowed" else "Following"
                                showFollowLabel = true
                                onFollowClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = if (showFollowLabel) followLabelText else if (isFollowed) "check" else "plus",
                            transitionSpec = {
                                (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith
                                    (fadeOut() + scaleOut(targetScale = 0.8f))
                            },
                            label = "followAnim"
                        ) { state ->
                            when (state) {
                                "Following" -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Following",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                "Unfollowed" -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.3f)
                                    ) {
                                        Text(
                                            text = "Unfollowed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                "check" -> {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Following",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                else -> { // "plus"
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                Icons.Rounded.Add,
                                                contentDescription = "Follow",
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Solid footer — station info
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station.genre.ifEmpty { "Radio" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- Equalizer Bars Animation ---

@Composable
private fun EqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(28.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { height ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(height)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// --- Gradient Fallback ---

@Composable
private fun GradientFallback(station: RadioStation) {
    val baseColor = station.accentColor
    val lighterShade = baseColor.copy(
        red = (baseColor.red * 1.5f).coerceAtMost(1f),
        green = (baseColor.green * 1.4f).coerceAtMost(1f),
        blue = (baseColor.blue * 1.6f).coerceAtMost(1f)
    )
    val darkerShade = baseColor.copy(
        red = baseColor.red * 0.3f,
        green = baseColor.green * 0.3f,
        blue = baseColor.blue * 0.3f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(lighterShade, baseColor, darkerShade)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = station.name.split(" ")
                .take(2)
                .joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
                .ifEmpty { station.name.take(2).uppercase() },
            style = MaterialTheme.typography.displaySmall,
            color = Color.White.copy(alpha = 0.12f),
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp
        )
    }
}
