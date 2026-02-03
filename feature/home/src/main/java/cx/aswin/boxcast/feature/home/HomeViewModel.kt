package cx.aswin.boxcast.feature.home

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.data.SubscriptionRepository
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import cx.aswin.boxcast.core.model.Episode
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

@Immutable
data class SmartHeroItem(
    val type: HeroType,
    val podcast: Podcast,
    val label: String,
    val description: String? = null,
    val gridItems: List<Podcast> = emptyList() // For RESUME_GRID
)

enum class HeroType { RESUME, RESUME_GRID, JUMP_BACK_IN, NEW_EPISODES_GRID, SPOTLIGHT }

@Immutable
data class HomeUiState(
    val heroItems: List<SmartHeroItem>,
    val latestEpisodes: List<Podcast> = emptyList(), // "Latest" Section
    val subscribedPodcasts: List<Podcast> = emptyList(), // "Your Shows" Section
    val selectedCategory: String? = null, // Null = "For You"
    val risingPodcasts: List<Podcast>, 
    val discoverPodcasts: List<Podcast>, 
    val isLoading: Boolean = false, // Initial full-screen loader
    val isFilterLoading: Boolean = false, // Inline loader when switching genres
    val isError: Boolean = false
)

class HomeViewModel(
    application: Application,
    apiBaseUrl: String,
    publicKey: String
) : AndroidViewModel(application) {
    
    private val repository = PodcastRepository(baseUrl = apiBaseUrl, publicKey = publicKey, context = application)
    private val database = cx.aswin.boxcast.core.data.database.BoxCastDatabase.getDatabase(application)
    private val subscriptionRepository = cx.aswin.boxcast.core.data.SubscriptionRepository(database.podcastDao())
    private val playbackRepository = cx.aswin.boxcast.core.data.PlaybackRepository(application, database.listeningHistoryDao())

    private val _uiState = MutableStateFlow(HomeUiState(emptyList(), emptyList(), emptyList(), null, emptyList(), emptyList(), isLoading = true, isFilterLoading = false))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    
    // Cached base data (For You)
    private var cachedForYouTrending: List<Podcast> = emptyList()
    private var cachedHeroItems: List<SmartHeroItem> = emptyList()
    private var cachedRisingPodcasts: List<Podcast> = emptyList()
    private var cachedLatestEpisodes: List<Podcast> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // --- BASE DATA FLOW (Only runs once, caches "For You" data) ---
            combine(
                repository.getTrendingPodcastsStream("in", 50, null) // Always "For You"
                    .onStart { 
                        android.util.Log.d("BoxCastTiming", "VM: Base stream starting")
                        emit(emptyList()) 
                    },
                playbackRepository.resumeSessions
                    .onStart { emit(emptyList()) },
                subscriptionRepository.subscribedPodcasts
                    .onStart { emit(emptyList()) }
            ) { trendingList, resumeList, subs ->
                 Triple(trendingList, resumeList, subs)
            }.collect { (trendingList, resumeList, subs) ->
                // ... (Existing Logic, but now dynamic) ...
                
                // Note: Resume/Hero logic shouldn't disappear when filtering genres...
                // User expectation: "News" tab shows News in the Grid.
                // Does it hide the "Resume" carousel?
                // Usually "For You" has Resume. "News" is just a feed.
                // But for simplicity, let's keep the Header/Resume always visible for now, or hide if it looks weird.
                // Let's keep it. Users might want to resume while browsing News.
                
                // ... Copying logic ...
                    
                    if (trendingList.isEmpty()) {
                         // Still loading or error, but we might have local data?
                         // Ideally we show local data even if trending is empty.
                         // For now, if streaming starts, it might emit empty first? No, emit 2 first.
                    }

                    // Proceed to build UI even with partial trending list
                    val heroList = mutableListOf<SmartHeroItem>()
                    val usedPodcastIds = mutableSetOf<String>()

                    // A. Real Resume (Priority 1) - The VERY last played
                    val lastPlayed = resumeList.firstOrNull()
                    if (lastPlayed != null) {
                        try {
                            // ZERO-NETWORK RESUME: Use cached metadata directly
                            val resumePodcast = Podcast(
                                id = lastPlayed.podcastId,
                                title = lastPlayed.podcastTitle, // Cached
                                artist = "", // Not critical for Hero Card header
                                imageUrl = lastPlayed.podcastImageUrl ?: "", // Primary Podcast Image
                                fallbackImageUrl = lastPlayed.podcastImageUrl, // Fallback
                                description = "",
                                genre = "Podcast",
                                latestEpisode = Episode(
                                    id = lastPlayed.episodeId,
                                    title = lastPlayed.episodeTitle, // Cached
                                    description = "",
                                    imageUrl = lastPlayed.imageUrl ?: "", // Episode Image
                                    audioUrl = lastPlayed.audioUrl ?: "", // Cached URL (Zero-Network)
                                    duration = (lastPlayed.durationMs / 1000).toInt(),
                                    publishedDate = 0L
                                )
                            )

                            val timeLeft = ((lastPlayed.durationMs - lastPlayed.positionMs) / 60000).coerceAtLeast(1)
                            heroList.add(
                                SmartHeroItem(
                                    type = HeroType.RESUME,
                                    podcast = resumePodcast,
                                    label = "RESUME • ${timeLeft}m left",
                                    description = lastPlayed.episodeTitle // Use cached title
                                )
                            )
                            usedPodcastIds.add(resumePodcast.id)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    
                    // B. Secondary Resume (Logic for 2 vs 3+)
                    if (resumeList.size == 2) {
                        // Special Case: If exactly 2, show the second one as a full Hero Card too
                        val secondSession = resumeList[1]
                        try {
                            val secondPodcast = Podcast(
                                id = secondSession.podcastId,
                                title = secondSession.podcastTitle,
                                artist = "",
                                imageUrl = secondSession.podcastImageUrl ?: "", // Podcast Image
                                fallbackImageUrl = secondSession.podcastImageUrl,
                                description = "",
                                genre = "Podcast",
                                latestEpisode = Episode(
                                    id = secondSession.episodeId,
                                    title = secondSession.episodeTitle,
                                    description = "",
                                    imageUrl = secondSession.imageUrl ?: "", // Episode Image
                                    audioUrl = secondSession.audioUrl ?: "",
                                    duration = (secondSession.durationMs / 1000).toInt(),
                                    publishedDate = 0L
                                )
                            )
                            val timeLeft = ((secondSession.durationMs - secondSession.positionMs) / 60000).coerceAtLeast(1)
                            heroList.add(
                                SmartHeroItem(
                                    type = HeroType.RESUME,
                                    podcast = secondPodcast,
                                    label = "RESUME • ${timeLeft}m left",
                                    description = secondSession.episodeTitle
                                )
                            )
                            usedPodcastIds.add(secondPodcast.id)
                        } catch (e: Exception) {}
                        
                    } else if (resumeList.size > 2) {
                        // Standard Case: Group the rest into a Bento Grid
                        val gridCandidates = resumeList.drop(1).take(6)
                        val gridPodcasts = mutableListOf<Podcast>()
                        
                        for (session in gridCandidates) {
                            // Calculate progress ratio (safe div)
                            val ratio = if (session.durationMs > 0) {
                                (session.positionMs.toFloat() / session.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            
                            val pod = Podcast(
                                id = session.podcastId,
                                title = session.podcastTitle,
                                artist = "", 
                                imageUrl = session.podcastImageUrl ?: "", // Podcast Image (Grid usually shows Podcast Art anyway)
                                fallbackImageUrl = session.podcastImageUrl,
                                description = "",
                                genre = "Podcast",
                                resumeProgress = ratio,
                                latestEpisode = Episode(
                                    id = session.episodeId,
                                    title = session.episodeTitle,
                                    description = "",
                                    imageUrl = session.imageUrl ?: "", // Episode Image
                                    audioUrl = session.audioUrl ?: "",
                                    duration = (session.durationMs / 1000).toInt(),
                                    publishedDate = 0L
                                )
                            )
                            gridPodcasts.add(pod)
                            usedPodcastIds.add(pod.id)
                        }
                        
                        if (gridPodcasts.isNotEmpty()) {
                            heroList.add(
                                SmartHeroItem(
                                    type = HeroType.RESUME_GRID,
                                    podcast = gridPodcasts.first(), 
                                    label = "JUMP BACK IN",
                                    description = null,
                                    gridItems = gridPodcasts
                                )
                            )
                        }
                    }

                    // C. Latest "Catch Up" List (New Episodes from Subs)
                    val catchUpList = mutableListOf<Podcast>()
                    if (subs.isNotEmpty()) {
                        try {
                             // 1. Identify candidates (subscribed but not already shown in Resume)
                            val candidates = subs.filter { !usedPodcastIds.contains(it.id) }
                            
                            if (candidates.isNotEmpty()) {
                                // 2. Sync top 20 candidates
                                val idsToSync = candidates.take(20).map { it.id }
                                val syncResults = repository.syncSubscriptions(idsToSync)
                                
                                // 3. Build List of "New" Episodes
                                for (pod in candidates) {
                                    val freshEpisode = syncResults[pod.id]
                                    if (freshEpisode != null) {
                                        // Check if actually new (not in history)
                                        val lastPlayedSession = resumeList.find { it.podcastId == pod.id }
                                        val isActuallyNew = lastPlayedSession == null || lastPlayedSession.episodeId != freshEpisode.id
                                        
                                        if (isActuallyNew) {
                                            catchUpList.add(pod.copy(latestEpisode = freshEpisode))
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    
                    // Logic for "Fresh Drops" (New Episodes)
                    if (catchUpList.isNotEmpty()) {
                         if (catchUpList.size > 2) {
                             // 1. Hero Card: Top Fresh Drop
                             val topDrop = catchUpList.first()
                             heroList.add(
                                SmartHeroItem(
                                    type = HeroType.JUMP_BACK_IN, // Reusing label/style for "NEW EPISODE"
                                    podcast = topDrop,
                                    label = "FRESH DROP",
                                    description = topDrop.latestEpisode?.title
                                )
                             )
                             usedPodcastIds.add(topDrop.id)
                             
                             // 2. Grid for the rest
                             val gridDrops = catchUpList.drop(1).take(6)
                             if (gridDrops.isNotEmpty()) {
                                 heroList.add(
                                     SmartHeroItem(
                                         type = HeroType.NEW_EPISODES_GRID,
                                         podcast = gridDrops.first(), // Pivot podcast (unused)
                                         label = "NEW EPISODES",
                                         description = null,
                                         gridItems = gridDrops
                                     )
                                 )
                                 usedPodcastIds.addAll(gridDrops.map { it.id })
                             }
                             
                             // User requested "New Episodes" section below Your Shows "as well".
                             // So we do NOT remove them from catchUpList here.
                             // catchUpList.removeAll { usedPodcastIds.contains(it.id) }
                             
                         } else {
                             // Simple Case: Just 1 or 2 cards?
                             val heroCandidate = catchUpList.first()
                             heroList.add(
                                 SmartHeroItem(
                                     type = HeroType.JUMP_BACK_IN,
                                     podcast = heroCandidate,
                                     label = "NEW EPISODE",
                                     description = heroCandidate.latestEpisode?.title
                                 )
                             )
                             usedPodcastIds.add(heroCandidate.id)
                             // Keep in list for Rail "as well"
                             // catchUpList.removeAt(0)
                         }
                    }

                    // C. Spotlight (Fill to 8)
                    var i = 0
                    while (heroList.size < 8 && i < trendingList.size) {
                        val pod = trendingList[i]
                        if (!usedPodcastIds.contains(pod.id)) {
                            val label = when {
                                i == 0 -> "#1 IN INDIA"
                                pod.genre.isNotEmpty() && !pod.genre.equals("Podcast", ignoreCase = true) -> "TRENDING IN ${pod.genre.uppercase()}"
                                else -> "TRENDING"
                            }
                            val spotlightDesc = pod.latestEpisode?.title ?: pod.genre
                            
                            // VISUAL CONSISTENCY: If showing Episode Title, show Episode Art
                            val latestEp = pod.latestEpisode
                            val epUrl = latestEp?.imageUrl
                            
                            val displayPodcast = if (!epUrl.isNullOrEmpty()) {
                                pod.copy(
                                    imageUrl = epUrl,
                                    fallbackImageUrl = pod.imageUrl // Fallback to Podcast Art
                                )
                            } else {
                                pod.copy(fallbackImageUrl = pod.imageUrl)
                            }
                            
                            heroList.add(
                                SmartHeroItem(
                                    type = HeroType.SPOTLIGHT,
                                    podcast = displayPodcast,
                                    label = label,
                                    description = spotlightDesc
                                )
                            )
                            usedPodcastIds.add(pod.id)
                        }
                        i++
                    }

                    val remaining = trendingList.filter { !usedPodcastIds.contains(it.id) }
                    val rising = remaining.take(10)
                    val discover = remaining.drop(10)

                    android.util.Log.e("BoxCastDebug", "--- UI UPDATE ---")
                    android.util.Log.e("BoxCastDebug", "TOTAL Received: ${trendingList.size}")
                    android.util.Log.e("BoxCastDebug", "Hero/Spotlight Used: ${usedPodcastIds.size}")
                    android.util.Log.e("BoxCastDebug", "Remaining: ${remaining.size}")
                    android.util.Log.e("BoxCastDebug", "Rising: ${rising.size}")
                    android.util.Log.e("BoxCastDebug", "Discover (Explore): ${discover.size}")
                    android.util.Log.e("BoxCastDebug", "-----------------")

                    // Cache the "For You" data
                    if (trendingList.isNotEmpty()) {
                        cachedForYouTrending = trendingList
                        cachedHeroItems = heroList
                        cachedRisingPodcasts = rising
                        cachedLatestEpisodes = catchUpList
                    }

                    // Update UI with base data
                    _uiState.value = HomeUiState(
                        heroItems = heroList,
                        latestEpisodes = catchUpList,
                        subscribedPodcasts = subs,
                        selectedCategory = _selectedCategory.value,
                        risingPodcasts = rising,
                        discoverPodcasts = discover,
                        isLoading = false,
                        isFilterLoading = false,
                        isError = false
                    )
                }
        }
        
        // --- CATEGORY OBSERVER (Separate from base data) ---
        viewModelScope.launch {
            _selectedCategory.collectLatest { category ->
                if (category == null) {
                    // "For You" - use cached data instantly
                    if (cachedHeroItems.isNotEmpty()) {
                        val discover = cachedForYouTrending.filter { pod ->
                            !cachedHeroItems.any { it.podcast.id == pod.id } &&
                            !cachedRisingPodcasts.any { it.id == pod.id }
                        }
                        
                        _uiState.update { 
                            it.copy(
                                selectedCategory = null,
                                discoverPodcasts = discover,
                                isFilterLoading = false
                            )
                        }
                    }
                } else {
                    // Category selected - show loader (inline) and fetch
                    _uiState.update { it.copy(
                        isFilterLoading = true, 
                        selectedCategory = category,
                        discoverPodcasts = emptyList() // Clear old data
                    ) }
                    
                    try {
                        // Collect all emissions and use the final (complete) list
                        // This avoids showing intermediate/partial data to the user
                        var finalList: List<Podcast> = emptyList()
                        repository.getTrendingPodcastsStream("in", 50, category)
                            .collect { items ->
                                finalList = items
                                // Update UI progressively but flag as still loading
                                // until stream completes
                                _uiState.update { 
                                    it.copy(
                                        discoverPodcasts = items,
                                        isFilterLoading = items.size < 10 // Show loader until we have reasonable data
                                    )
                                }
                            }
                        // Final update after stream completes
                        _uiState.update { 
                            it.copy(
                                discoverPodcasts = finalList,
                                isFilterLoading = false
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Category stream error", e)
                        _uiState.update { it.copy(isFilterLoading = false) }
                    }
                }
            }
        }
    }
    
    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
    
    fun toggleSubscription(podcastId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val podcast = state.risingPodcasts.find { it.id == podcastId }
                ?: state.discoverPodcasts.find { it.id == podcastId }
                ?: state.heroItems.find { it.podcast.id == podcastId }?.podcast
            
            if (podcast != null) {
                subscriptionRepository.toggleSubscription(podcast)
            }
        }
    }

    fun deleteHistoryItem(episodeId: String) {
        viewModelScope.launch {
            playbackRepository.deleteSession(episodeId)
        }
    }
    
    // Debug Accessors
    val debugHistory = playbackRepository.getAllHistory()
    val debugPodcasts = subscriptionRepository.getAllSubscribedPodcasts()
}
