package cx.aswin.boxcast.feature.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cx.aswin.boxcast.feature.home.SmartHeroItem

import cx.aswin.boxcast.core.model.Podcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroCarousel(
    heroItems: List<SmartHeroItem>,
    onPlayClick: (Podcast) -> Unit,
    onDetailsClick: (Podcast) -> Unit,
    onArrowClick: (SmartHeroItem) -> Unit,
    onToggleSubscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (heroItems.isEmpty()) return

    val carouselState = rememberCarouselState { heroItems.size }
    
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 320.dp, 
        itemSpacing = 16.dp, 
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) { i ->
        val item = heroItems[i]
        
        if (item.type == cx.aswin.boxcast.feature.home.HeroType.RESUME_GRID) {
            HeroGridCard(
                items = item.gridItems,
                title = "JUMP BACK IN",
                onPlayClick = { onPlayClick(it) },
                onDetailsClick = { podcast ->
                    // For grid items, we want Episode Details. 
                    // We need to pass this action up.
                    // Let's assume onDetailsClick passed to HeroCarousel handles this.
                    onDetailsClick(podcast)
                },
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
            )
        } else if (item.type == cx.aswin.boxcast.feature.home.HeroType.NEW_EPISODES_GRID) {
             HeroGridCard(
                items = item.gridItems,
                title = "NEW EPISODES",
                onPlayClick = { onPlayClick(it) },
                onDetailsClick = { podcast ->
                    // Same details logic
                    onDetailsClick(podcast)
                },
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
            )
        } else {
            HeroCard(
                item = item,
                onClick = { onPlayClick(item.podcast) }, // Primary "Play" button action
                onArrowClick = { onArrowClick(item) },
                onToggleSubscription = { onToggleSubscription(item.podcast.id) },
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
            )
        }
    }
}

