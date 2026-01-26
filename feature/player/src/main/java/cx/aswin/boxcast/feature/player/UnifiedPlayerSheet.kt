package cx.aswin.boxcast.feature.player

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.MutatorMutex
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import cx.aswin.boxcast.core.data.PlaybackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.SideEffect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// Constants matching PixelPlayer
val MiniPlayerHeight = 64.dp
private val MiniPlayerBottomSpacer = 8.dp
private const val ANIMATION_DURATION_MS = 255

enum class PlayerSheetState { COLLAPSED, EXPANDED }

/**
 * Extracts a seed color from a bitmap using Palette API (like PixelPlayer)
 */
private suspend fun extractSeedColor(bitmap: Bitmap): Color = withContext(Dispatchers.Default) {
    val scaledBitmap = if (bitmap.width > 100 || bitmap.height > 100) {
        val scale = 100f / max(bitmap.width, bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    } else {
        bitmap
    }
    
    val palette = Palette.Builder(scaledBitmap)
        .maximumColorCount(16)
        .generate()
    
    val color = palette.vibrantSwatch?.rgb?.let { Color(it) }
        ?: palette.mutedSwatch?.rgb?.let { Color(it) }
        ?: palette.dominantSwatch?.rgb?.let { Color(it) }
        ?: Color(0xFF6750A4) // Default purple
    
    if (scaledBitmap != bitmap) {
        scaledBitmap.recycle()
    }
    
    color
}

/**
 * Generates a color scheme from a seed color (simplified version of PixelPlayer)
 */
private fun generateColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seedColor.red * 255).toInt(),
        (seedColor.green * 255).toInt(),
        (seedColor.blue * 255).toInt(),
        hsl
    )
    
    fun hslToColor(h: Float, s: Float, l: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
    }
    
    val hue = hsl[0]
    val saturation = hsl[1].coerceIn(0.3f, 0.7f)
    
    return if (isDark) {
        darkColorScheme(
            primary = hslToColor(hue, saturation, 0.7f),
            onPrimary = hslToColor(hue, saturation * 0.3f, 0.15f),
            primaryContainer = hslToColor(hue, saturation * 0.5f, 0.25f),
            onPrimaryContainer = hslToColor(hue, saturation * 0.3f, 0.9f),
            surface = hslToColor(hue, saturation * 0.2f, 0.12f),
            onSurface = hslToColor(hue, saturation * 0.1f, 0.9f),
            surfaceVariant = hslToColor(hue, saturation * 0.3f, 0.2f),
            onSurfaceVariant = hslToColor(hue, saturation * 0.2f, 0.75f),
        )
    } else {
        lightColorScheme(
            primary = hslToColor(hue, saturation, 0.4f),
            onPrimary = hslToColor(hue, saturation * 0.2f, 0.95f),
            primaryContainer = hslToColor(hue, saturation * 0.6f, 0.9f),
            onPrimaryContainer = hslToColor(hue, saturation * 0.5f, 0.15f),
            surface = hslToColor(hue, saturation * 0.3f, 0.96f),
            onSurface = hslToColor(hue, saturation * 0.2f, 0.1f),
            surfaceVariant = hslToColor(hue, saturation * 0.4f, 0.92f),
            onSurfaceVariant = hslToColor(hue, saturation * 0.3f, 0.3f),
        )
    }
}

/**
 * Unified Player Sheet - Exact replication of PixelPlayer's architecture.
 * 
 * Features:
 * 1. Album art color extraction with Palette API
 * 2. Animated expansion with lerp
 * 3. Mini and Full player content with alpha crossfade
 * 4. Drag gestures for expand/collapse
 * 5. Proper navbar overlap when expanded
 */
@Composable
fun UnifiedPlayerSheet(
    playbackRepository: PlaybackRepository,
    sheetCollapsedTargetY: Float,
    containerHeight: Dp,
    collapsedStateHorizontalPadding: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val state by playbackRepository.playerState.collectAsState()
    val episode = state.currentEpisode
    val podcast = state.currentPodcast
    
    // Don't render if no episode
    if (episode == null) return
    
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val isDarkTheme = isSystemInDarkTheme()
    val window = (context as? android.app.Activity)?.window

    SideEffect {
        window?.let { win ->
             val insetsController = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
             // Force transparent bars
             win.statusBarColor = android.graphics.Color.TRANSPARENT
             win.navigationBarColor = android.graphics.Color.TRANSPARENT
             
             // Light/Dark icons based on theme
             insetsController.isAppearanceLightStatusBars = !isDarkTheme
             insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    // Color extraction state
    var extractedColorScheme by remember { mutableStateOf<ColorScheme?>(null) }
    val colorScheme = extractedColorScheme ?: MaterialTheme.colorScheme
    
    // Load and extract colors from album art
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(episode.imageUrl)
            .size(Size(100, 100))
            .allowHardware(false) // Required for Palette
            .build()
    )
    
    LaunchedEffect(episode.imageUrl, painter.state) {
        val painterState = painter.state
        if (painterState is AsyncImagePainter.State.Success) {
            val bitmap = (painterState.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val seedColor = extractSeedColor(bitmap)
                extractedColorScheme = generateColorScheme(seedColor, isDarkTheme)
            }
        }
    }
    
    // Sheet state (internal)
    var currentSheetContentState by remember { mutableStateOf(PlayerSheetState.COLLAPSED) }
    
    // Core expansion fraction (0f = collapsed, 1f = expanded)
    val playerContentExpansionFraction = remember { Animatable(0f) }
    val sheetAnimationMutex = remember { MutatorMutex() }
    
    // Visual overshoot for bounce effect
    val visualOvershootScaleY = remember { Animatable(1f) }
    
    // Screen dimensions
    val screenHeightPx = remember(configuration, density) { 
        with(density) { configuration.screenHeightDp.dp.toPx() } 
    }
    val miniPlayerHeightPx = remember(density) { with(density) { MiniPlayerHeight.toPx() } }
    val sheetExpandedTargetY = 0f
    
    // Animation spec
    val sheetAnimationSpec = remember {
        tween<Float>(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    }
    
    // Sheet translation Y
    val initialY = if (currentSheetContentState == PlayerSheetState.COLLAPSED) sheetCollapsedTargetY else sheetExpandedTargetY
    val currentSheetTranslationY = remember { Animatable(initialY) }
    
    // Derived values using lerp
    val playerContentAreaHeightDp by remember {
        derivedStateOf {
            lerp(MiniPlayerHeight, containerHeight, playerContentExpansionFraction.value)
        }
    }
    
    // Mini player corner radius (matching PixelPlayer - rounded square/squircle)
    val overallSheetTopCornerRadius by remember {
        derivedStateOf {
            val collapsedCorner = 32.dp  // Squircle-like corners when collapsed
            val expandedCorner = 0.dp
            lerp(collapsedCorner, expandedCorner, playerContentExpansionFraction.value)
        }
    }
    
    val playerContentBottomRadius by remember {
        derivedStateOf {
            lerp(32.dp, 0.dp, playerContentExpansionFraction.value)
        }
    }
    
    val currentHorizontalPadding by remember {
        derivedStateOf {
            lerp(collapsedStateHorizontalPadding, 0.dp, playerContentExpansionFraction.value)
        }
    }
    
    val playerAreaElevation by remember {
        derivedStateOf {
            lerp(3.dp, 16.dp, playerContentExpansionFraction.value)
        }
    }
    
    // Mini player alpha
    val miniAlpha by remember {
        derivedStateOf {
            (1f - playerContentExpansionFraction.value * 2f).coerceIn(0f, 1f)
        }
    }
    
    // Full player alpha
    val fullPlayerContentAlpha by remember {
        derivedStateOf {
            ((playerContentExpansionFraction.value - 0.25f).coerceIn(0f, 0.75f) / 0.75f)
        }
    }
    
    val initialFullPlayerOffsetY = remember(density) { with(density) { 24.dp.toPx() } }
    val fullPlayerTranslationY by remember {
        derivedStateOf {
            lerp(initialFullPlayerOffsetY, 0f, fullPlayerContentAlpha)
        }
    }
    
    // Animate player sheet function
    suspend fun animatePlayerSheet(
        targetExpanded: Boolean,
        animationSpec: androidx.compose.animation.core.AnimationSpec<Float> = sheetAnimationSpec,
        initialVelocity: Float = 0f
    ) {
        val targetFraction = if (targetExpanded) 1f else 0f
        val targetY = if (targetExpanded) sheetExpandedTargetY else sheetCollapsedTargetY
        val velocityScale = (sheetCollapsedTargetY - sheetExpandedTargetY).coerceAtLeast(1f)
        
        sheetAnimationMutex.mutate {
            coroutineScope {
                launch {
                    currentSheetTranslationY.animateTo(
                        targetValue = targetY,
                        initialVelocity = initialVelocity,
                        animationSpec = animationSpec
                    )
                }
                launch {
                    playerContentExpansionFraction.animateTo(
                        targetValue = targetFraction,
                        initialVelocity = initialVelocity / velocityScale,
                        animationSpec = animationSpec
                    )
                }
            }
        }
    }
    
    // Back handler
    BackHandler(enabled = currentSheetContentState == PlayerSheetState.EXPANDED) {
        scope.launch {
            launch {
                val currentFraction = playerContentExpansionFraction.value
                val initialSquash = lerp(1.0f, 0.97f, currentFraction)
                visualOvershootScaleY.snapTo(initialSquash)
                visualOvershootScaleY.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                )
            }
            launch { animatePlayerSheet(targetExpanded = false) }
            currentSheetContentState = PlayerSheetState.COLLAPSED
        }
    }
    
    // Drag state
    var isDragging by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }
    var accumulatedDragYSinceStart by remember { mutableFloatStateOf(0f) }
    
    // The sheet Surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, currentSheetTranslationY.value.roundToInt()) }
            .height(containerHeight),
        shadowElevation = 0.dp,
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Player content area with drag handling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = currentHorizontalPadding)
                    .height(playerContentAreaHeightDp)
                    .graphicsLayer {
                        scaleY = visualOvershootScaleY.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .shadow(
                        elevation = playerAreaElevation,
                        shape = RoundedCornerShape(
                            topStart = overallSheetTopCornerRadius,
                            topEnd = overallSheetTopCornerRadius,
                            bottomStart = playerContentBottomRadius,
                            bottomEnd = playerContentBottomRadius
                        ),
                        clip = false
                    )
                    .background(
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(
                            topStart = overallSheetTopCornerRadius,
                            topEnd = overallSheetTopCornerRadius,
                            bottomStart = playerContentBottomRadius,
                            bottomEnd = playerContentBottomRadius
                        )
                    )
                    .clipToBounds()
                    .pointerInput(Unit) {
                        var initialFractionOnDragStart = 0f
                        var initialYOnDragStart = 0f
                        
                        detectVerticalDragGestures(
                            onDragStart = {
                                scope.launch {
                                    currentSheetTranslationY.stop()
                                    playerContentExpansionFraction.stop()
                                }
                                isDragging = true
                                velocityTracker.resetTracking()
                                initialFractionOnDragStart = playerContentExpansionFraction.value
                                initialYOnDragStart = currentSheetTranslationY.value
                                accumulatedDragYSinceStart = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDragYSinceStart += dragAmount
                                scope.launch {
                                    val newY = (currentSheetTranslationY.value + dragAmount)
                                        .coerceIn(
                                            sheetExpandedTargetY - miniPlayerHeightPx * 0.2f,
                                            sheetCollapsedTargetY + miniPlayerHeightPx * 0.2f
                                        )
                                    currentSheetTranslationY.snapTo(newY)
                                    
                                    val denom = (sheetCollapsedTargetY - sheetExpandedTargetY).coerceAtLeast(1f)
                                    val dragRatio = (initialYOnDragStart - newY) / denom
                                    val newFraction = (initialFractionOnDragStart + dragRatio).coerceIn(0f, 1f)
                                    playerContentExpansionFraction.snapTo(newFraction)
                                }
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                            },
                            onDragEnd = {
                                isDragging = false
                                
                                val verticalVelocity = velocityTracker.calculateVelocity().y
                                val currentFraction = playerContentExpansionFraction.value
                                val minDragThresholdPx = with(density) { 5.dp.toPx() }
                                val velocityThreshold = 150f
                                
                                val targetState = when {
                                    abs(accumulatedDragYSinceStart) > minDragThresholdPx ->
                                        if (accumulatedDragYSinceStart < 0) PlayerSheetState.EXPANDED else PlayerSheetState.COLLAPSED
                                    abs(verticalVelocity) > velocityThreshold ->
                                        if (verticalVelocity < 0) PlayerSheetState.EXPANDED else PlayerSheetState.COLLAPSED
                                    else ->
                                        if (currentFraction > 0.5f) PlayerSheetState.EXPANDED else PlayerSheetState.COLLAPSED
                                }
                                
                                scope.launch {
                                    if (targetState == PlayerSheetState.EXPANDED) {
                                        launch { animatePlayerSheet(targetExpanded = true) }
                                        currentSheetContentState = PlayerSheetState.EXPANDED
                                    } else {
                                        val dynamicDamping = lerp(
                                            Spring.DampingRatioNoBouncy,
                                            Spring.DampingRatioLowBouncy,
                                            currentFraction
                                        )
                                        launch {
                                            val initialSquash = lerp(1.0f, 0.97f, currentFraction)
                                            visualOvershootScaleY.snapTo(initialSquash)
                                            visualOvershootScaleY.animateTo(
                                                1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessVeryLow
                                                )
                                            )
                                        }
                                        launch {
                                            animatePlayerSheet(
                                                targetExpanded = false,
                                                animationSpec = spring(
                                                    dampingRatio = dynamicDamping,
                                                    stiffness = Spring.StiffnessLow
                                                ),
                                                initialVelocity = verticalVelocity
                                            )
                                        }
                                        currentSheetContentState = PlayerSheetState.COLLAPSED
                                    }
                                }
                                accumulatedDragYSinceStart = 0f
                            }
                        )
                    }
                    .clickable(
                        enabled = true,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            if (currentSheetContentState == PlayerSheetState.COLLAPSED) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                launch { animatePlayerSheet(targetExpanded = true) }
                                currentSheetContentState = PlayerSheetState.EXPANDED
                            } else {
                                launch {
                                    val currentFraction = playerContentExpansionFraction.value
                                    val initialSquash = lerp(1.0f, 0.97f, currentFraction)
                                    visualOvershootScaleY.snapTo(initialSquash)
                                    visualOvershootScaleY.animateTo(
                                        1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessVeryLow
                                        )
                                    )
                                }
                                launch { animatePlayerSheet(targetExpanded = false) }
                                currentSheetContentState = PlayerSheetState.COLLAPSED
                            }
                        }
                    }
            ) {
                // Color scheme crossfade like PixelPlayer
                Crossfade(
                    targetState = colorScheme,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "playerColorScheme"
                ) { scheme ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // MINI PLAYER with swipe-to-dismiss
                        SwipeableMiniPlayer(
                            isPlaying = state.isPlaying,
                            onDismiss = { playbackRepository.clearSession() },
                            backgroundColor = scheme.primaryContainer,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer { alpha = miniAlpha }
                                .zIndex(if (playerContentExpansionFraction.value < 0.5f) 1f else 0f)
                        ) {
                            MiniPlayerContent(
                                episode = episode,
                                podcastTitle = podcast?.title ?: "",
                                podcastImageUrl = podcast?.imageUrl,
                                isPlaying = state.isPlaying,
                                isLoading = state.isLoading,
                                position = state.position,
                                duration = state.duration,
                                colorScheme = scheme,
                                onPlayPause = {
                                    if (state.isPlaying) playbackRepository.pause()
                                    else playbackRepository.resume()
                                },
                                onPrevious = { playbackRepository.skipBackward() },
                                onNext = { playbackRepository.skipForward() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // FULL PLAYER
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = fullPlayerContentAlpha
                                    translationY = fullPlayerTranslationY
                                }
                                .zIndex(if (playerContentExpansionFraction.value >= 0.5f) 1f else 0f)
                                .offset { if (playerContentExpansionFraction.value <= 0.01f) IntOffset(0, 10000) else IntOffset.Zero }
                        ) {
                            FullPlayerContent(
                                playbackRepository = playbackRepository,
                                colorScheme = scheme,
                                onCollapse = {
                                    scope.launch {
                                        launch {
                                            val currentFraction = playerContentExpansionFraction.value
                                            val initialSquash = lerp(1.0f, 0.97f, currentFraction)
                                            visualOvershootScaleY.snapTo(initialSquash)
                                            visualOvershootScaleY.animateTo(
                                                1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessVeryLow
                                                )
                                            )
                                        }
                                        launch { animatePlayerSheet(targetExpanded = false) }
                                        currentSheetContentState = PlayerSheetState.COLLAPSED
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Swipeable wrapper for mini player - only swipeable when paused
 * Player slides to reveal dismiss pill behind it
 */
@Composable
private fun SwipeableMiniPlayer(
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color,
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
        androidx.compose.animation.AnimatedVisibility(
            visible = showConfirmPill,
            enter = androidx.compose.animation.fadeIn(tween(200)),
            exit = androidx.compose.animation.fadeOut(tween(150)),
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

@Composable
private fun MiniPlayerContent(
    episode: cx.aswin.boxcast.core.model.Episode,
    podcastTitle: String,
    podcastImageUrl: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: Long,
    duration: Long,
    colorScheme: ColorScheme,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Box(modifier = modifier) {
        // Main content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .padding(start = 10.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art - circular with fallback to podcast image
            val imageUrl = episode.imageUrl?.takeIf { it.isNotBlank() } ?: podcastImageUrl
            AsyncImage(
                model = imageUrl,
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant)
                    .graphicsLayer { alpha = if (isLoading) 0.6f else 1f }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title and podcast
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    ),
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Expressive Seek Buttons with coupled animations (PixelPlayer-inspired)
            val seekBackScale = remember { Animatable(1f) }
            val seekForwardScale = remember { Animatable(1f) }
            val scope = rememberCoroutineScope()
            
            // Seek Back -10s button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { 
                        scaleX = seekBackScale.value
                        scaleY = seekBackScale.value
                    }
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        enabled = !isLoading
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Animate this button (bounce) + subtle nudge on sibling
                        scope.launch {
                            launch {
                                seekBackScale.animateTo(0.8f, tween(80))
                                seekBackScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                            launch {
                                // Subtle coupled reaction on the other button
                                seekForwardScale.animateTo(0.95f, tween(60))
                                seekForwardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
                            }
                        }
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Replay10,
                    contentDescription = "Seek back 10 seconds",
                    tint = colorScheme.primary.copy(alpha = if (isLoading) 0.5f else 1f),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Play/Pause button - pulses when buffering (M3 Expressive)
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAnimatedScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val pulseScale = if (isLoading) pulseAnimatedScale else 1f
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        enabled = !isLoading
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colorScheme.onPrimary.copy(alpha = if (isLoading) 0.6f else 1f),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Seek Forward +30s button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { 
                        scaleX = seekForwardScale.value
                        scaleY = seekForwardScale.value
                    }
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        enabled = !isLoading
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Animate this button (bounce) + subtle nudge on sibling
                        scope.launch {
                            launch {
                                seekForwardScale.animateTo(0.8f, tween(80))
                                seekForwardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                            launch {
                                // Subtle coupled reaction on the other button
                                seekBackScale.animateTo(0.95f, tween(60))
                                seekBackScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
                            }
                        }
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Forward30,
                    contentDescription = "Seek forward 30 seconds",
                    tint = colorScheme.primary.copy(alpha = if (isLoading) 0.5f else 1f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // Progress bar at bottom - standard LinearProgressIndicator
        if (duration > 0) {
            val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun FullPlayerContent(
    playbackRepository: PlaybackRepository,
    colorScheme: ColorScheme,
    onCollapse: () -> Unit
) {
    val state by playbackRepository.playerState.collectAsState()
    val episode = state.currentEpisode ?: return
    val podcast = state.currentPodcast
    
    // Use passed colorScheme which is already adaptive
    val containerColor = colorScheme.primaryContainer.copy(alpha = 0.6f).compositeOver(colorScheme.surface) // Blend for depth
    val controlTint = colorScheme.primary
    
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDarkTheme = isSystemInDarkTheme()
    
    // Manage system bars
    val window = (LocalContext.current as? android.app.Activity)?.window
    SideEffect {
        window?.let { win ->
             val insetsController = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
             win.statusBarColor = android.graphics.Color.TRANSPARENT
             win.navigationBarColor = android.graphics.Color.TRANSPARENT
             insetsController.isAppearanceLightStatusBars = !isDarkTheme
             insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(top = statusBarPadding, bottom = navBarPadding)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorScheme.onSurface.copy(alpha = 0.1f))
                    .clickable(onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.weight(1f))
        
            Box(modifier = Modifier.size(42.dp))
        }
        
        Spacer(modifier = Modifier.height(12.dp)) // Tight gap
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 1. Album Art
            val imageUrl = episode.imageUrl?.takeIf { it.isNotBlank() } ?: podcast?.imageUrl
            val context = LocalContext.current
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f, fill = false)
                    .aspectRatio(1f)
                    .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = controlTint.copy(alpha = 0.3f), spotColor = controlTint.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .allowHardware(false) // Required for Palette
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { 
                        // Colors extracted in parent
                    }
                )
            }
            
        Spacer(modifier = Modifier.height(16.dp)) // Reduced Buffer
        
        // 2. Metadata
        Column(modifier = Modifier.fillMaxWidth().height(60.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), // Reduced from titleLarge
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip, // Must be Clip for marquee
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast?.title ?: "",
                style = MaterialTheme.typography.bodyMedium, // Reduced from bodyLarge
                color = colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp)) // Tight gap
            
            // 3. Linear Buffered Slider
            if (state.duration > 0) {
                val bufferedPercentage = (state.bufferedPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
                
                LinearBufferedSlider(
                    position = state.position,
                    duration = state.duration,
                    bufferedPercentage = bufferedPercentage,
                    onSeek = { playbackRepository.seekTo(it) },
                    color = controlTint
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 4. Split Control Layout (Rows)
            
            // 4. Pixel Weighted Control Layout
            val interactionSourcePrev = remember { MutableInteractionSource() }
            val interactionSourcePlay = remember { MutableInteractionSource() }
            val interactionSourceNext = remember { MutableInteractionSource() }
            
            var lastClickedId by remember { mutableStateOf<Int?>(null) }
            
            // Auto-reset last clicked
            LaunchedEffect(lastClickedId) {
                if (lastClickedId != null) {
                    kotlinx.coroutines.delay(220)
                    lastClickedId = null
                }
            }
            
            val baseWeight = 1f
            val expansionWeight = 1.15f
            val compressionWeight = 0.85f
            
            fun getWeight(id: Int): Float = when (lastClickedId) {
                id -> expansionWeight
                null -> baseWeight
                else -> compressionWeight
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // Reduced from 96dp (User request: "reduce height", "pill shaped")
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // REPLAY -10s
                val weightPrev by animateFloatAsState(getWeight(0), label = "prevW")
                Box(
                    modifier = Modifier
                        .weight(weightPrev)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(48.dp)) // Pill Shape
                        .background(colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(interactionSource = interactionSourcePrev, indication = ripple(), onClick = {
                            lastClickedId = 0
                            playbackRepository.skipBackward()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Replay 10s",
                        modifier = Modifier.size(32.dp),
                        tint = controlTint
                    )
                }

                // PLAY / PAUSE (Morphing Squircle)
                val weightPlay by animateFloatAsState(getWeight(1), label = "playW")
                val isPlaying = state.isPlaying
                val isLoading = state.isLoading
                
                // Animate Corner Radius: 32.dp (Square-ish) when Playing <-> 48.dp (Round) when Paused
                // Pixel Logic: Playing = Squarer (to match pause bars?), Paused = Rounder (Play triangle?)
                // Actually usually: Play button is Round. Pause button is Round or Squircle.
                // Let's stick to Reference: "if (!visualState) cornerPlaying (60) else cornerPaused (26)"
                // User Request: "Pill shaped" (less boxy). Row height 96dp -> Radius 48dp = Full Pill.
                // Playing: Slightly squarer (32dp). Paused: Full Pill (48dp).
                val cornerRadius by animateDpAsState(
                    targetValue = if (isPlaying) 32.dp else 48.dp,
                    label = "corner"
                )
                
                Box(
                    modifier = Modifier
                        .weight(weightPlay)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(controlTint) // Vibrant/Primary
                        .clickable(interactionSource = interactionSourcePlay, indication = ripple(), onClick = {
                            lastClickedId = 1
                            if (isPlaying) playbackRepository.pause() else playbackRepository.resume()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        BoxCastLoader.CircularWavy(
                            modifier = Modifier.size(48.dp), // Increased size
                            size = 48.dp,
                            color = colorScheme.onPrimary // Fix color mismatch
                        )
                    } else {
                         Crossfade(targetState = isPlaying) { playing ->
                             Icon(
                                 imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                 contentDescription = if (playing) "Pause" else "Play",
                                 modifier = Modifier.size(38.dp),
                                 tint = colorScheme.onPrimary
                             )
                         }
                    }
                }

                // FORWARD +30s
                val weightNext by animateFloatAsState(getWeight(2), label = "nextW")
                Box(
                    modifier = Modifier
                        .weight(weightNext)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(48.dp)) // Pill Shape
                        .background(colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(interactionSource = interactionSourceNext, indication = ripple(), onClick = {
                            lastClickedId = 2
                            playbackRepository.skipForward()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward30,
                        contentDescription = "Forward 30s",
                        modifier = Modifier.size(32.dp),
                        tint = controlTint
                    )
                }
            }


        }
    }
}

@Composable
private fun LinearBufferedSlider(
    position: Long,
    duration: Long,
    bufferedPercentage: Float,
    onSeek: (Long) -> Unit,
    color: Color
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
            // Buffer Track
            androidx.compose.material3.LinearProgressIndicator(
                progress = { bufferedPercentage },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color.copy(alpha = 0.3f), // Visible buffer
                trackColor = color.copy(alpha = 0.1f), // Faint bg
            )
            
            // Interactive Slider
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color.Transparent, // Transparent to show buffer
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
        
        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun animateFloatAsStateWithInteraction(
    interactionSource: MutableInteractionSource,
    pressedScale: Float
): androidx.compose.runtime.State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
