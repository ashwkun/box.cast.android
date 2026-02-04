package cx.aswin.boxcast.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.data.SubscriptionRepository
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExploreUiState {
    data object Loading : ExploreUiState
    data class Success(
        val trending: List<Podcast> = emptyList(),
        val searchResults: List<Podcast> = emptyList(),
        val subscribedIds: Set<String> = emptySet(), // For badging
        val currentCategory: String = "All",
        val searchQuery: String = "",
        val isSearching: Boolean = false,
        val isLoading: Boolean = false // For showing skeleton in grid area only
    ) : ExploreUiState
    data class Error(val message: String) : ExploreUiState
}

class ExploreViewModel(
    application: android.app.Application,
    private val podcastRepository: PodcastRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsHelper: cx.aswin.boxcast.core.data.analytics.AnalyticsHelper,
    initialCategory: String? = null // New param
) : androidx.lifecycle.AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    // Internal state to combine
    private val _searchQuery = MutableStateFlow("")
    private val _currentCategory = MutableStateFlow(initialCategory ?: "All") // Use it here
    private val _trendingPodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    private val _searchResults = MutableStateFlow<List<Podcast>>(emptyList())
    private val _isLoading = MutableStateFlow(true) // Explicit loading state
    
    // Search Job to cancel previous searches
    private var searchJob: Job? = null

    init {
        // Observe Subscriptions for Badging
        viewModelScope.launch {
            combine(
                subscriptionRepository.subscribedPodcastIds,
                _currentCategory,
                _trendingPodcasts,
                _searchResults,
                _searchQuery,
                _isLoading
            ) { args: Array<Any?> ->
                val subIds = args[0] as Set<String>
                val category = args[1] as String
                val trending = args[2] as List<Podcast>
                val searchRes = args[3] as List<Podcast>
                val query = args[4] as String
                val pIsLoading = args[5] as Boolean

                val isSearching = query.isNotEmpty()

                // Always emit Success state, but with isLoading flag
                // This allows the screen to show the real header during initial load
                ExploreUiState.Success(
                    trending = trending,
                    searchResults = searchRes,
                    subscribedIds = subIds,
                    currentCategory = category,
                    searchQuery = query,
                    isSearching = isSearching,
                    isLoading = pIsLoading && trending.isEmpty() && searchRes.isEmpty()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Search Debounce Implementation
        startSearchObserver()
        
        // Initial Load
        loadTrending(_currentCategory.value)
    }

    @OptIn(FlowPreview::class)
    private fun startSearchObserver() {
        _searchQuery
            .debounce(500L) // Wait 500ms after last char
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        if (_currentCategory.value == category) return
        _currentCategory.value = category
        // Clear Search when switching category to browse
        _searchQuery.value = "" 
        _trendingPodcasts.value = emptyList() // Clear to force Skeleton
        loadTrending(category)
    }

    private fun loadTrending(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Map "All" to null for API, and lowercase others for consistency
                val apiCategory = if (category == "All") null else category.lowercase()
                
                // This hits the Turso DB (via Proxy)
                val podcasts = podcastRepository.getTrendingPodcasts(
                    country = "us", 
                    limit = 50,
                    category = apiCategory
                )
                _trendingPodcasts.value = podcasts
            } catch (e: Exception) {
                // Handle error
                _trendingPodcasts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = emptyList() // Clear previous results to force Skeleton
            try {
                // Log Search Event
                analyticsHelper.logSearch(query)

                // This hits the Hybrid Proxy (Turso + API)
                val results = podcastRepository.searchPodcasts(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error silently for search
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
