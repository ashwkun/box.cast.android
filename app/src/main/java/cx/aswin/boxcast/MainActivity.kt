package cx.aswin.boxcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cx.aswin.boxcast.core.designsystem.component.BoxCastNavigationBar
import cx.aswin.boxcast.core.designsystem.component.bottomNavDestinations
import cx.aswin.boxcast.core.designsystem.theme.BoxCastTheme
import cx.aswin.boxcast.feature.home.HomeRoute
import cx.aswin.boxcast.feature.player.PlayerRoute

// PixelPlayer-inspired transition specs
private const val TRANSITION_DURATION = 500
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
                
                // Playback Repository (Singleton-ish for UI)
                val application = (applicationContext as android.app.Application)
                val database = remember { cx.aswin.boxcast.core.data.database.BoxCastDatabase.getDatabase(application) }
                val playbackRepository = remember { cx.aswin.boxcast.core.data.PlaybackRepository(application, database.listeningHistoryDao()) }
                
                Scaffold(
                    bottomBar = {
                        Column {
                            // Mini Player (Global)
                            if (!currentRoute.startsWith("player")) {
                                cx.aswin.boxcast.feature.player.BoxCastMiniPlayer(
                                    playbackRepository = playbackRepository,
                                    onExpand = {
                                        // Expand to player screen with current podcast context
                                        val state = playbackRepository.playerState.value
                                        state.currentPodcast?.let { podcast ->
                                            navController.navigate("player/${podcast.id}")
                                        }
                                    }
                                )
                            }
                            
                            if (showBottomNav) {
                                BoxCastNavigationBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            // Pop up to the start destination of the graph to
                                            // avoid building up a large stack of destinations
                                            popUpTo("home") {
                                                saveState = true
                                            }
                                            // Avoid multiple copies of the same destination
                                            launchSingleTop = true
                                            // Restore state when reselecting a previously selected item
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding),
                        // Push: Enter from Right
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                                initialOffsetX = { it }
                            )
                        },
                        // Push: Exit to Left with Fade (Parallax - only moves 1/3)
                        exitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                                targetOffsetX = { -it / 3 }
                            ) + fadeOut(
                                animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING)
                            )
                        },
                        // Pop: Enter from Left (Parallax)
                        popEnterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                                initialOffsetX = { -it / 3 }
                            ) + fadeIn(
                                animationSpec = tween(TRANSITION_DURATION / 2, easing = TRANSITION_EASING)
                            )
                        },
                        // Pop: Exit to Right
                        popExitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                                targetOffsetX = { it }
                            ) + fadeOut(
                                animationSpec = tween(TRANSITION_DURATION / 2, easing = TRANSITION_EASING)
                            )
                        }
                    ) {
                        // Main tabs
                        composable("home") {
                            HomeRoute(
                                apiBaseUrl = apiBaseUrl,
                                apiKey = apiKey,
                                onPodcastClick = { podcast ->
                                    // Navigate to Podcast Info page (not Player)
                                    navController.navigate("podcast/${podcast.id}")
                                },
                                onPlayClick = { podcast -> 
                                    // Navigate directly to Player (Resume)
                                    navController.navigate("player/${podcast.id}")
                                },
                                onHeroArrowClick = { heroItem ->
                                    // Navigate to Episode Info with data from SmartHeroItem
                                    val ep = heroItem.podcast.latestEpisode
                                    if (ep != null) {
                                        // Use "_" placeholder for empty/null strings to avoid // in URL path
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
                                        // Fallback: no episode data, go to podcast info
                                        navController.navigate("podcast/${heroItem.podcast.id}")
                                    }
                                },
                                onEpisodeClick = { episode, podcast ->
                                    // Navigate to Episode Info from "New Episodes" section
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
                                    // Navigate to Library tab
                                    navController.navigate("library") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        
                        composable("explore") {
                            PlaceholderScreen(title = "Explore")
                        }
                        
                        composable("library") {
                            PlaceholderScreen(title = "Library")
                        }
                        
                        // Detail screens
                        composable(
                            route = "player/{podcastId}",
                            arguments = listOf(
                                navArgument("podcastId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val podcastId = backStackEntry.arguments?.getString("podcastId") ?: return@composable
                            PlayerRoute(
                                podcastId = podcastId,
                                apiBaseUrl = apiBaseUrl,
                                apiKey = apiKey,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // Podcast Info Screen
                        composable(
                            route = "podcast/{podcastId}",
                            arguments = listOf(
                                navArgument("podcastId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val podcastId = backStackEntry.arguments?.getString("podcastId") ?: return@composable
                            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<cx.aswin.boxcast.feature.info.PodcastInfoViewModel>(
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return cx.aswin.boxcast.feature.info.PodcastInfoViewModel(
                                            application,
                                            apiBaseUrl,
                                            apiKey
                                        ) as T
                                    }
                                }
                            )
                            cx.aswin.boxcast.feature.info.PodcastInfoScreen(
                                podcastId = podcastId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { episode ->
                                    // Navigate to EpisodeInfo (needs serialized data)
                                    navController.navigate(
                                        "episode/${episode.id}/${java.net.URLEncoder.encode(episode.title, "UTF-8")}/" +
                                        "${java.net.URLEncoder.encode(episode.description.take(500), "UTF-8")}/" +
                                        "${java.net.URLEncoder.encode(episode.imageUrl, "UTF-8")}/" +
                                        "${java.net.URLEncoder.encode(episode.audioUrl, "UTF-8")}/" +
                                        "${episode.duration}/${podcastId}/" +
                                        java.net.URLEncoder.encode(viewModel.uiState.value.let { 
                                            if (it is cx.aswin.boxcast.feature.info.PodcastInfoUiState.Success) it.podcast.title else "Podcast" 
                                        }, "UTF-8")
                                    )
                                },
                                onPlayEpisode = { episode ->
                                    // Start playback directly
                                    navController.navigate("player/$podcastId")
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
                                        return cx.aswin.boxcast.feature.info.EpisodeInfoViewModel(
                                            application,
                                            apiBaseUrl,
                                            apiKey
                                        ) as T
                                    }
                                }
                            )
                            // Decode helper that converts "_" placeholder back to empty string
                            fun decode(s: String?) = java.net.URLDecoder.decode(s ?: "", "UTF-8").let { if (it == "_") "" else it }
                            cx.aswin.boxcast.feature.info.EpisodeInfoScreen(
                                episodeId = args.getString("episodeId") ?: "",
                                episodeTitle = decode(args.getString("episodeTitle")),
                                episodeDescription = decode(args.getString("episodeDescription")),
                                episodeImageUrl = decode(args.getString("episodeImageUrl")),
                                episodeAudioUrl = decode(args.getString("episodeAudioUrl")),
                                episodeDuration = args.getInt("episodeDuration"),
                                podcastId = args.getString("podcastId") ?: "",
                                podcastTitle = decode(args.getString("podcastTitle")),
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pId -> navController.navigate("podcast/$pId") },
                                onPlay = {
                                    // TODO: Start playback with episode context
                                    navController.navigate("player/${args.getString("podcastId")}")
                                }
                            )
                        }
                    }
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
