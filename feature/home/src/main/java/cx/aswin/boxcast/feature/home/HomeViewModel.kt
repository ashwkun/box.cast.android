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
    
    private val userPrefs = cx.aswin.boxcast.core.data.UserPreferencesRepository(application)
    
    // Expose region to UI
    val currentRegion = userPrefs.regionStream
    
    // Cached base data (For You)
    private var cachedForYouTrending: List<Podcast> = emptyList()
    private var cachedHeroItems: List<SmartHeroItem> = emptyList()
    private var cachedRisingPodcasts: List<Podcast> = emptyList()
    private var cachedLatestEpisodes: List<Podcast> = emptyList()
    
    // Store current region for use in other scopes
    private var activeRegion = "us"

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // --- BASE DATA FLOW (Restarts when Region changes) ---
            userPrefs.regionStream.collectLatest { region ->
                activeRegion = region
                
                combine(
                    repository.getTrendingPodcastsStream(region, 50, null) // Dynamic Region
                        .onStart { 
                            android.util.Log.d("BoxCastTiming", "VM: Base stream starting for region=$region")
                            emit(emptyList()) 
                        },
                    playbackRepository.resumeSessions
                        .onStart { emit(emptyList()) },
                    subscriptionRepository.subscribedPodcasts
                        .onStart { emit(emptyList()) }
                ) { trendingList, resumeList, subs ->
                     Triple(trendingList, resumeList, subs)
                }.collect { (trendingList, resumeList, subs) ->
                    // ... (Logic copied below) ...
                    
                    // Note: Resume/Hero logic shouldn't disappear when filtering genres...
                    // ...
                    
                        if (trendingList.isEmpty()) {
                             // Still loading
                        }

                        // Proceed to build UI even with partial trending list
                        val heroList = mutableListOf<SmartHeroItem>()
                        val usedPodcastIds = mutableSetOf<String>()

                        // A. Real Resume (Priority 1)
                        val lastPlayed = resumeList.firstOrNull()
                        if (lastPlayed != null) {
                            try {
                                val resumePodcast = Podcast(
                                    id = lastPlayed.podcastId,
                                    title = lastPlayed.podcastTitle,
                                    artist = "",
                                    imageUrl = lastPlayed.podcastImageUrl ?: "",
                                    fallbackImageUrl = lastPlayed.podcastImageUrl,
                                    description = "",
                                    genre = "Podcast",
                                    latestEpisode = Episode(
                                        id = lastPlayed.episodeId,
                                        title = lastPlayed.episodeTitle,
                                        description = "",
                                        imageUrl = lastPlayed.imageUrl ?: "",
                                        audioUrl = lastPlayed.audioUrl ?: "",
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
                                        description = lastPlayed.episodeTitle
                                    )
                                )
                                usedPodcastIds.add(resumePodcast.id)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        
                        // B. Secondary Resume
                        if (resumeList.size == 2) {
                            val secondSession = resumeList[1]
                            try {
                                val secondPodcast = Podcast(
                                    id = secondSession.podcastId,
                                    title = secondSession.podcastTitle,
                                    artist = "",
                                    imageUrl = secondSession.podcastImageUrl ?: "",
                                    fallbackImageUrl = secondSession.podcastImageUrl,
                                    description = "",
                                    genre = "Podcast",
                                    latestEpisode = Episode(
                                        id = secondSession.episodeId,
                                        title = secondSession.episodeTitle,
                                        description = "",
                                        imageUrl = secondSession.imageUrl ?: "",
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
                            val gridCandidates = resumeList.drop(1).take(6)
                            val gridPodcasts = mutableListOf<Podcast>()
                            
                            for (session in gridCandidates) {
                                val ratio = if (session.durationMs > 0) {
                                    (session.positionMs.toFloat() / session.durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                
                                val pod = Podcast(
                                    id = session.podcastId,
                                    title = session.podcastTitle,
                                    artist = "", 
                                    imageUrl = session.podcastImageUrl ?: "",
                                    fallbackImageUrl = session.podcastImageUrl,
                                    description = "",
                                    genre = "Podcast",
                                    resumeProgress = ratio,
                                    latestEpisode = Episode(
                                        id = session.episodeId,
                                        title = session.episodeTitle,
                                        description = "",
                                        imageUrl = session.imageUrl ?: "",
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

                        // C. Latest "Catch Up" List
                        val catchUpList = mutableListOf<Podcast>()
                        if (subs.isNotEmpty()) {
                            try {
                                val candidates = subs.filter { !usedPodcastIds.contains(it.id) }
                                
                                if (candidates.isNotEmpty()) {
                                    val idsToSync = candidates.take(20).map { it.id }
                                    val syncResults = repository.syncSubscriptions(idsToSync)
                                    
                                    for (pod in candidates) {
                                        val freshEpisode = syncResults[pod.id]
                                        if (freshEpisode != null) {
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
                        
                        if (catchUpList.isNotEmpty()) {
                             if (catchUpList.size > 2) {
                                 val topDrop = catchUpList.first()
                                 heroList.add(
                                    SmartHeroItem(
                                        type = HeroType.JUMP_BACK_IN,
                                        podcast = topDrop,
                                        label = "FRESH DROP",
                                        description = topDrop.latestEpisode?.title
                                    )
                                 )
                                 usedPodcastIds.add(topDrop.id)
                                 
                                 val gridDrops = catchUpList.drop(1).take(6)
                                 if (gridDrops.isNotEmpty()) {
                                     heroList.add(
                                         SmartHeroItem(
                                             type = HeroType.NEW_EPISODES_GRID,
                                             podcast = gridDrops.first(), 
                                             label = "NEW EPISODES",
                                             description = null,
                                             gridItems = gridDrops
                                         )
                                     )
                                     usedPodcastIds.addAll(gridDrops.map { it.id })
                                 }
                             } else {
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
                             }
                        }

                        // C. Spotlight (Fill to 8)
                        var i = 0
                        while (heroList.size < 8 && i < trendingList.size) {
                            val pod = trendingList[i]
                            if (!usedPodcastIds.contains(pod.id)) {
                                val label = when {
                                    i == 0 -> if (region == "in") "#1 IN INDIA" else "#1 IN US"
                                    pod.genre.isNotEmpty() && !pod.genre.equals("Podcast", ignoreCase = true) -> "TRENDING IN ${pod.genre.uppercase()}"
                                    else -> "TRENDING"
                                }
                                val spotlightDesc = pod.latestEpisode?.title ?: pod.genre
                                
                                val latestEp = pod.latestEpisode
                                val epUrl = latestEp?.imageUrl
                                
                                val displayPodcast = if (!epUrl.isNullOrEmpty()) {
                                    pod.copy(
                                        imageUrl = epUrl,
                                        fallbackImageUrl = pod.imageUrl 
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

                        if (trendingList.isNotEmpty()) {
                            cachedForYouTrending = trendingList
                            cachedHeroItems = heroList
                            cachedRisingPodcasts = rising
                            cachedLatestEpisodes = catchUpList
                        }

                        _uiState.value = HomeUiState(
                            heroItems = heroList,
                            latestEpisodes = catchUpList,
                            subscribedPodcasts = subs,
                            selectedCategory = _selectedCategory.value,
                            risingPodcasts = rising,
                            discoverPodcasts = discover,
                            isLoading = false,
                            isFilterLoading = trendingList.isEmpty(),
                            isError = false
                        )
                }
            }
        }
        
        // --- CATEGORY OBSERVER (Considers Region) ---
        viewModelScope.launch {
            combine(_selectedCategory, userPrefs.regionStream) { category, region -> 
                category to region 
            }.collectLatest { (category, region) ->
                if (category == null) {
                    // "For You" - use cached data instantly (if matches region?)
                    // Simplified: caching matches current region because region change triggers base reload
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
                    // Category selected
                    _uiState.update { it.copy(
                        isFilterLoading = true, 
                        selectedCategory = category,
                        discoverPodcasts = emptyList()
                    ) }
                    
                    try {
                        android.util.Log.d("HomeViewModel", "Category: Fetching '$category' for region '$region'...")
                        var finalList: List<Podcast> = emptyList()
                        repository.getTrendingPodcastsStream(region, 50, category.lowercase())
                            .collect { items ->
                                finalList = items
                                _uiState.update { 
                                    it.copy(
                                        discoverPodcasts = items,
                                        isFilterLoading = items.size < 10
                                    )
                                }
                            }
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
    
    fun setRegion(region: String) {
        viewModelScope.launch {
            userPrefs.setRegion(region)
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
