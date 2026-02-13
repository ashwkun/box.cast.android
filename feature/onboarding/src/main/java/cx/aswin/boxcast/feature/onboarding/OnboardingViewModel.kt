package cx.aswin.boxcast.feature.onboarding

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.data.SubscriptionRepository
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.GENRES,
    val selectedGenres: Set<String> = emptySet(),
    val recommendedPodcasts: List<Podcast> = emptyList(),
    val subscribedPodcastIds: Set<String> = emptySet(),
    val isLoadingPodcasts: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Podcast> = emptyList(),
    val isSearching: Boolean = false,
    val isCompleting: Boolean = false
)

enum class OnboardingStep {
    GENRES, PODCASTS, SEARCH
}

class OnboardingViewModel(
    application: Application,
    private val podcastRepository: PodcastRepository,
    private val subscriptionRepository: SubscriptionRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("boxcast_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }
    
    
    
    fun toggleGenre(genre: String) {
        _uiState.update { state ->
            val newGenres = if (genre in state.selectedGenres) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newGenres)
        }
    }
    
    fun continueToRecommendations() {
        _uiState.update { it.copy(currentStep = OnboardingStep.PODCASTS, isLoadingPodcasts = true) }
        viewModelScope.launch {
            val selectedGenres = _uiState.value.selectedGenres.toList()
            val allPodcasts = mutableListOf<Podcast>()
            
            // Fetch trending podcasts for each selected genre
            val perGenreLimit = when {
                selectedGenres.size <= 2 -> 5
                selectedGenres.size <= 4 -> 3
                else -> 2
            }
            
            for (genre in selectedGenres) {
                val trending = podcastRepository.getTrendingPodcasts(
                    category = genre,
                    limit = perGenreLimit
                )
                allPodcasts.addAll(trending)
            }
            
            // Deduplicate and limit to 10
            val uniquePodcasts = allPodcasts
                .distinctBy { it.id }
                .shuffled()
                .take(10)
            
            _uiState.update {
                it.copy(
                    recommendedPodcasts = uniquePodcasts,
                    subscribedPodcastIds = uniquePodcasts.map { p -> p.id }.toSet(), // Pre-select all
                    isLoadingPodcasts = false
                )
            }
        }
    }
    
    fun togglePodcastSubscription(podcastId: String) {
        _uiState.update { state ->
            val newSubs = if (podcastId in state.subscribedPodcastIds) {
                state.subscribedPodcastIds - podcastId
            } else {
                state.subscribedPodcastIds + podcastId
            }
            state.copy(subscribedPodcastIds = newSubs)
        }
    }
    
    fun navigateToSearch() {
        _uiState.update { it.copy(currentStep = OnboardingStep.SEARCH) }
    }
    
    fun navigateBackFromSearch() {
        _uiState.update { state ->
            val backStep = if (state.selectedGenres.isNotEmpty()) OnboardingStep.PODCASTS else OnboardingStep.GENRES
            state.copy(currentStep = backStep, searchQuery = "", searchResults = emptyList())
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        // Debounce search
        searchJob?.cancel()
        val cleaned = query.trim()
        if (cleaned.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(400) // Debounce
            val results = podcastRepository.searchPodcasts(cleaned)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
    
    fun subscribeFromSearch(podcast: Podcast) {
        _uiState.update { state ->
            // Add to subscribed IDs
            val newSubs = state.subscribedPodcastIds + podcast.id
            
            // Allow this podcast to appear in the main recommendation list too
            val newRecommendations = if (state.recommendedPodcasts.any { it.id == podcast.id }) {
                state.recommendedPodcasts
            } else {
                state.recommendedPodcasts + podcast
            }
            
            state.copy(
                subscribedPodcastIds = newSubs,
                recommendedPodcasts = newRecommendations
            )
        }
        // Metadata is already captured in the podcast object added to recommendations.
        // Actual DB write will happen in completeOnboarding to avoid double-toggling issues.
    }
    
    fun completeOnboarding(onDone: () -> Unit) {
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            val state = _uiState.value
            // Subscribe to all selected podcasts from recommendations (which now includes search selections)
            val podcastsToSubscribe = state.recommendedPodcasts.filter { it.id in state.subscribedPodcastIds }
            for (podcast in podcastsToSubscribe) {
                // Use idempotent subscribe to avoid toggling off existing subs
                subscriptionRepository.subscribe(podcast)
            }
            
            // Mark onboarding as completed
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            
            // Save selected genres for future personalization
            prefs.edit().putStringSet("user_genres", state.selectedGenres).apply()
            
            onDone()
        }
    }
    
    fun skipOnboarding(onDone: () -> Unit) {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        onDone()
    }
}
