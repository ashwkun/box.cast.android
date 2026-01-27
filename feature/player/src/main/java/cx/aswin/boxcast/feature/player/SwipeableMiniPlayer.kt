package cx.aswin.boxcast.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch

/**
 * Swipeable wrapper for mini player - only swipeable when paused
 * Player slides to reveal dismiss pill behind it
 */
@Composable
fun SwipeableMiniPlayer(
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // Swipe state
    val offsetX = remember { Animatable(0f) }
    var showConfirmPill by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf(0) } // -1 = left, 1 = right
    var autoHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Thresholds
    val dismissThreshold = with(density) { 100.dp.toPx() }
    
    // Static outer container - doesn't move
    Box(modifier = modifier) {
        // Dismiss pill BEHIND the player (visible when player slides away)
        AnimatedVisibility(
            visible = showConfirmPill,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (swipeDirection > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            autoHideJob?.cancel()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Dismiss",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Player content - THIS moves with swipe (with its own background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .pointerInput(isPlaying) {
                    if (isPlaying) return@pointerInput // No swipe when playing
                    
                    detectHorizontalDragGestures(
                        onDragStart = { 
                            autoHideJob?.cancel()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (kotlin.math.abs(offsetX.value) > dismissThreshold) {
                                    // Show confirm pill
                                    swipeDirection = if (offsetX.value < 0) -1 else 1
                                    showConfirmPill = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    // Animate to reveal pill
                                    offsetX.animateTo(
                                        targetValue = swipeDirection * dismissThreshold * 1.5f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    
                                    // Auto-restore after 3 seconds
                                    autoHideJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(3000)
                                        showConfirmPill = false
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                } else {
                                    // Snap back
                                    showConfirmPill = false
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                showConfirmPill = false
                                offsetX.animateTo(0f, spring())
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                                // Show pill when user swipes far enough
                                if (kotlin.math.abs(offsetX.value) > dismissThreshold * 0.5f && !showConfirmPill) {
                                    swipeDirection = if (offsetX.value < 0) -1 else 1
                                    showConfirmPill = true
                                }
                                // Hide pill if user drags back
                                if (showConfirmPill && kotlin.math.abs(offsetX.value) < dismissThreshold * 0.3f) {
                                    showConfirmPill = false
                                    autoHideJob?.cancel()
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
