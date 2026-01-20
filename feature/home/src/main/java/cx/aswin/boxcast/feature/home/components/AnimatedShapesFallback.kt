package cx.aswin.boxcast.feature.home.components

import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AnimatedShapesFallback() {
    // Random positions calculated once per composition
    val shapes = remember {
        val allShapes = listOf(
            ExpressiveShapes.Sunny, ExpressiveShapes.VerySunny, 
            ExpressiveShapes.Cookie4, ExpressiveShapes.Cookie6, ExpressiveShapes.Cookie9, ExpressiveShapes.Cookie12,
            ExpressiveShapes.Burst, ExpressiveShapes.SoftBurst, ExpressiveShapes.Boom, ExpressiveShapes.SoftBoom,
            ExpressiveShapes.Flower, ExpressiveShapes.Puffy, ExpressiveShapes.PuffyDiamond,
            ExpressiveShapes.Heart, ExpressiveShapes.Bun, ExpressiveShapes.GhostIsh,
            ExpressiveShapes.Diamond, ExpressiveShapes.Gem, ExpressiveShapes.Pentagon
        ).shuffled()
        
        val placedShapes = mutableListOf<Triple<Float, Float, androidx.compose.ui.graphics.Shape>>()
        val availableShapes = allShapes.toMutableList()
        
        // Try to place up to 6 shapes without overlapping
        for (i in 0 until 100) {
            if (placedShapes.size >= 6 || availableShapes.isEmpty()) break
            
            // Generate in a larger virtual space 350x600 for fallback
            val x = Random.nextFloat() * 350f
            val y = Random.nextFloat() * 600f
            
            var overlaps = false
            for (placed in placedShapes) {
                val px = placed.first
                val py = placed.second
                val dist = kotlin.math.sqrt((x - px) * (x - px) + (y - py) * (y - py))
                if (dist < 200f) { // Larger separation for fallback
                    overlaps = true
                    break
                }
            }
            
            if (!overlaps) {
                placedShapes.add(Triple(x, y, availableShapes.removeFirst()))
            }
        }
        placedShapes
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .graphicsLayer { clip = true } // Allow clipping at container edge
    ) {
        shapes.forEach { (dx, dy, shape) ->
            // Massive sizes
            val size = (180 + Random.nextInt(170)).dp 
            
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(
                        x = dx.dp - (size / 2), 
                        y = dy.dp - (size / 2)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), // Very subtle
                        shape = shape
                    )
            )
        }
        
        // Centered Podcast Icon
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Podcasts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
