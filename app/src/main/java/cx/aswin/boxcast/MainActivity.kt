package cx.aswin.boxcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.net.URLDecoder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cx.aswin.boxcast.core.designsystem.component.BoxCastNavigationBar
import cx.aswin.boxcast.core.designsystem.component.bottomNavDestinations
import cx.aswin.boxcast.core.designsystem.theme.BoxCastTheme
import cx.aswin.boxcast.core.designsystem.component.PredictiveBackWrapper
import cx.aswin.boxcast.feature.home.HomeRoute
import cx.aswin.boxcast.feature.player.PlayerRoute

// PixelPlayer-inspired transition specs
private const val TRANSITION_DURATION = 350
private val TRANSITION_EASING = FastOutSlowInEasing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxCastTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"
                
                // API config from BuildConfig
                val apiBaseUrl = BuildConfig.BOXCAST_API_BASE_URL
                val apiKey = BuildConfig.BOXCAST_API_KEY
                
                // Show bottom nav on all screens except player
                val showBottomNav = !currentRoute.startsWith("player")
                
                // Check if we can go back (for predictive back)
                val canGoBack = navController.previousBackStackEntry != null
                
                // Playback Repository (Singleton-ish for UI)
                val application = (applicationContext as android.app.Application)
                val database = remember { cx.aswin.boxcast.core.data.database.BoxCastDatabase.getDatabase(application) }
                val playbackRepository = remember { cx.aswin.boxcast.core.data.PlaybackRepository(application, database.listeningHistoryDao()) }
                
                val scope = rememberCoroutineScope() // Scope for playback actions
                
                // Restore last session on app startup
                LaunchedEffect(Unit) {
                    playbackRepository.restoreLastSession()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surface // Match content background
                    ) { innerPadding ->
                        PredictiveBackWrapper(
                            enabled = canGoBack,
                            onBack = { navController.popBackStack() }
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier, // No padding(innerPadding) -> Fixes GAP issue
                                enterTransition = { slideInHorizontally(animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING), initialOffsetX = { it }) },
                                exitTransition = { slideOutHorizontally(animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING), targetOffsetX = { -it / 3 }) + fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING)) },
                                popEnterTransition = { slideInHorizontally(animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING), initialOffsetX = { -it / 3 }) + fadeIn(animationSpec = tween(TRANSITION_DURATION / 2, easing = TRANSITION_EASING)) },
                                popExitTransition = { slideOutHorizontally(animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING), targetOffsetX = { it }) + fadeOut(animationSpec = tween(TRANSITION_DURATION / 2, easing = TRANSITION_EASING)) }
                            ) {
                            // Main tabs
                            composable("home") {
                                HomeRoute(
                                    apiBaseUrl = apiBaseUrl,
                                    apiKey = apiKey,
                                    onPodcastClick = { podcast ->
                                        navController.navigate("podcast/${podcast.id}")
                                    },
                                    onPlayClick = { podcast -> 
                                        // Start Playback
                                        val episode = podcast.latestEpisode
                                        if (episode != null) {
                                            scope.launch {
                                                playbackRepository.playEpisode(episode, podcast)
                                            }
                                        }
                                        // Do not navigate, just play. Mini player appears.
                                    },
                                    onHeroArrowClick = { heroItem ->
                                        val ep = heroItem.podcast.latestEpisode
                                        if (ep != null) {
                                            fun encode(s: String?) = java.net.URLEncoder.encode(s?.ifEmpty { "_" } ?: "_", "UTF-8")
                                            navController.navigate(
                                                "episode/${ep.id}/${encode(ep.title)}/" +
                                                "${encode(ep.description.take(500))}/" +
                                                "${encode(ep.imageUrl)}/" +
                                                "${encode(ep.audioUrl)}/" +
                                                "${ep.duration}/${heroItem.podcast.id}/" +
                                                encode(heroItem.podcast.title)
                                            )
                                        } else {
                                            navController.navigate("podcast/${heroItem.podcast.id}")
                                        }
                                    },
                                    onEpisodeClick = { episode, podcast ->
                                        fun encode(s: String?) = java.net.URLEncoder.encode(s?.ifEmpty { "_" } ?: "_", "UTF-8")
                                        navController.navigate(
                                            "episode/${episode.id}/${encode(episode.title)}/" +
                                            "${encode(episode.description.take(500))}/" +
                                            "${encode(episode.imageUrl)}/" +
                                            "${encode(episode.audioUrl)}/" +
                                            "${episode.duration}/${podcast.id}/" +
                                            encode(podcast.title)
                                        )
                                    },
                                    onNavigateToLibrary = {
                                        navController.navigate("library") {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            
                            composable("explore") { PlaceholderScreen(title = "Explore") }
                            composable("library") { PlaceholderScreen(title = "Library") }
                            
                            // REMOVED PlayerRoute logic from NavGraph

                            // Podcast Info Screen
                            composable(route = "podcast/{podcastId}", arguments = listOf(navArgument("podcastId") { type = NavType.StringType })) { backStackEntry ->
                                val podcastId = backStackEntry.arguments?.getString("podcastId") ?: return@composable
                                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<cx.aswin.boxcast.feature.info.PodcastInfoViewModel>(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return cx.aswin.boxcast.feature.info.PodcastInfoViewModel(application, apiBaseUrl, apiKey) as T
                                        }
                                    }
                                )
                                    // Calculate bottom padding for Mini Player
                                    // PlayerState is a data class. If currentEpisode is not null, player is active.
                                    val playerState by playbackRepository.playerState.collectAsState()
                                    val isPlayerVisible = playerState.currentEpisode != null
                                    
                                    // Base: NavBar clearance (64dp) + optional MiniPlayer (56dp)
                                    val miniPlayerPadding = if (isPlayerVisible) (64 + 56).dp else 64.dp
                                    
                                    cx.aswin.boxcast.feature.info.PodcastInfoScreen(
                                        podcastId = podcastId,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        bottomContentPadding = miniPlayerPadding,
                                        onEpisodeClick = { episode ->
                                            fun encode(s: String?) = java.net.URLEncoder.encode(s?.ifEmpty { "_" } ?: "_", "UTF-8")
                                            navController.navigate(
                                                "episode/${episode.id}/${encode(episode.title)}/" +
                                                "${encode(episode.description.take(500))}/" +
                                                "${encode(episode.imageUrl)}/" +
                                                "${encode(episode.audioUrl)}/" +
                                                "${episode.duration}/${podcastId}/" +
                                                encode(viewModel.uiState.value.let { if (it is cx.aswin.boxcast.feature.info.PodcastInfoUiState.Success) it.podcast.title else "Podcast" })
                                            )
                                        },
                                        onPlayEpisode = { episode ->
                                            // Start Playback -> Mini Player
                                            val state = viewModel.uiState.value
                                            if (state is cx.aswin.boxcast.feature.info.PodcastInfoUiState.Success) {
                                                scope.launch {
                                                    playbackRepository.playEpisode(episode, state.podcast)
                                                }
                                            }
                                        }
                                    )
                            }

                            // Episode Info Screen
                            composable(
                                route = "episode/{episodeId}/{episodeTitle}/{episodeDescription}/{episodeImageUrl}/{episodeAudioUrl}/{episodeDuration}/{podcastId}/{podcastTitle}",
                                arguments = listOf(
                                    navArgument("episodeId") { type = NavType.StringType },
                                    navArgument("episodeTitle") { type = NavType.StringType },
                                    navArgument("episodeDescription") { type = NavType.StringType },
                                    navArgument("episodeImageUrl") { type = NavType.StringType },
                                    navArgument("episodeAudioUrl") { type = NavType.StringType },
                                    navArgument("episodeDuration") { type = NavType.IntType },
                                    navArgument("podcastId") { type = NavType.StringType },
                                    navArgument("podcastTitle") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val args = backStackEntry.arguments ?: return@composable
                                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<cx.aswin.boxcast.feature.info.EpisodeInfoViewModel>(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return cx.aswin.boxcast.feature.info.EpisodeInfoViewModel(application, apiBaseUrl, apiKey) as T
                                        }
                                    }
                                )
                                fun decode(s: String?) = java.net.URLDecoder.decode(s ?: "", "UTF-8").let { if (it == "_") "" else it }
                                
                                val podcastId = args.getString("podcastId") ?: ""
                                val podcastTitle = decode(args.getString("podcastTitle"))
                                val episodeId = args.getString("episodeId") ?: ""
                                val episodeTitle = decode(args.getString("episodeTitle"))
                                
                                cx.aswin.boxcast.feature.info.EpisodeInfoScreen(
                                    episodeId = episodeId,
                                    episodeTitle = episodeTitle,
                                    episodeDescription = decode(args.getString("episodeDescription")),
                                    episodeImageUrl = decode(args.getString("episodeImageUrl")),
                                    episodeAudioUrl = decode(args.getString("episodeAudioUrl")),
                                    episodeDuration = args.getInt("episodeDuration"),
                                    podcastId = podcastId,
                                    podcastTitle = podcastTitle,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onPodcastClick = { pId -> navController.navigate("podcast/$pId") },
                                    onPlay = {
                                        // Construct objects for playback
                                        val episode = cx.aswin.boxcast.core.model.Episode(
                                            id = episodeId,
                                            title = episodeTitle,
                                            description = "",
                                            imageUrl = decode(args.getString("episodeImageUrl")),
                                            audioUrl = decode(args.getString("episodeAudioUrl")),
                                            duration = args.getInt("episodeDuration"),
                                            publishedDate = 0L
                                        )
                                        val podcast = cx.aswin.boxcast.core.model.Podcast(
                                            id = podcastId,
                                            title = podcastTitle,
                                            artist = "",
                                            imageUrl = "", // We might not have this here, but repository cache will fill it if available, or we use episode art
                                            description = "",
                                            genre = ""
                                        )
                                        scope.launch {
                                            playbackRepository.playEpisode(episode, podcast)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    }

                    // Calculate sheet positions
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current
                    val screenHeightDp = configuration.screenHeightDp.dp
                    
                    // Get system nav bar height for full-screen expanded player
                    val systemNavBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    
                    // Only app navbar height - reduced for tighter positioning
                    val appNavBarHeight = 24.dp 
                    
                    // Container height = full screen + system nav bar + extra buffer to ensure full coverage
                    val containerHeight = screenHeightDp + systemNavBarHeight + 50.dp
                    
                    // Collapsed position: mini player sits directly above app navbar
                    val collapsedTargetY = with(density) {
                        (screenHeightDp - cx.aswin.boxcast.feature.player.MiniPlayerHeight - appNavBarHeight).toPx()
                    }
                    

                    // Navigation Bar
                    if (showBottomNav) {
                        BoxCastNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Unified Player Sheet - PixelPlayer architecture (Last so it draws ON TOP)
                    cx.aswin.boxcast.feature.player.UnifiedPlayerSheet(
                        playbackRepository = playbackRepository,
                        sheetCollapsedTargetY = collapsedTargetY,
                        containerHeight = containerHeight,
                        collapsedStateHorizontalPadding = 12.dp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Text(
        text = "Box.Cast",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BoxCastTheme {
        Greeting()
    }
}
