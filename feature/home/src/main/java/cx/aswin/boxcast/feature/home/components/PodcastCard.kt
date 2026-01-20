package cx.aswin.boxcast.feature.home.components

import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import androidx.compose.ui.draw.clip


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import cx.aswin.boxcast.core.model.Podcast

@Composable
fun PodcastCard(
    podcast: Podcast,
    isTall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard( 
        // onClick = onClick, // Removed Material Click to use expressiveClickable
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .expressiveClickable(onClick = onClick)
    ) {
        Column {
            // Simple fallback: Podcast Image → AnimatedShapesFallback
            SubcomposeAsyncImage(
                model = podcast.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTall) 280.dp else 220.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                val state = painter.state
                if (state is coil.compose.AsyncImagePainter.State.Loading || 
                    state is coil.compose.AsyncImagePainter.State.Error || 
                    podcast.imageUrl.isEmpty()) {
                    cx.aswin.boxcast.feature.home.components.AnimatedShapesFallback()
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            
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
