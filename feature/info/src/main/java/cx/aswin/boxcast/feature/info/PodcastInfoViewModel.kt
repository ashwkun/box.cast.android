package cx.aswin.boxcast.feature.info

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EpisodeSort { NEWEST, OLDEST }

@Immutable
sealed interface PodcastInfoUiState {
    data object Loading : PodcastInfoUiState
    data class Success(
        val podcast: Podcast,
        val episodes: List<Episode>,
        val isSubscribed: Boolean,
        val isLoadingMore: Boolean = false,
        val hasMoreEpisodes: Boolean = true,
        val currentSort: EpisodeSort = EpisodeSort.NEWEST,
        val searchQuery: String = "",
        val isSearching: Boolean = false,
        val searchResults: List<Episode>? = null // null = not searching, empty = no results
    ) : PodcastInfoUiState
    data object Error : PodcastInfoUiState
}

class PodcastInfoViewModel(
    application: Application,
    private val apiBaseUrl: String,
    private val apiKey: String
) : AndroidViewModel(application) {

    private val repository = PodcastRepository(
        baseUrl = apiBaseUrl,
        apiKey = apiKey,
        context = application
    )
    private val database = cx.aswin.boxcast.core.data.database.BoxCastDatabase.getDatabase(application)
    private val subscriptionRepository = cx.aswin.boxcast.core.data.SubscriptionRepository(database.podcastDao())

    private val _uiState = MutableStateFlow<PodcastInfoUiState>(PodcastInfoUiState.Loading)
    val uiState: StateFlow<PodcastInfoUiState> = _uiState.asStateFlow()

    private var currentPodcastId: String = ""
    private var currentOffset: Int = 0
    private var searchJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    fun loadPodcast(podcastId: String) {
        currentPodcastId = podcastId
        currentOffset = 0
        viewModelScope.launch {
            _uiState.value = PodcastInfoUiState.Loading
            try {
                val podcast = repository.getPodcastDetails(podcastId)
                if (podcast != null) {
                    val page = repository.getEpisodesPaginated(podcastId, PAGE_SIZE, 0, "newest")
                    val isSubscribed = subscriptionRepository.isSubscribed(podcastId)
                    currentOffset = page.episodes.size
                    _uiState.value = PodcastInfoUiState.Success(
                        podcast = podcast,
                        episodes = page.episodes,
                        isSubscribed = isSubscribed,
                        hasMoreEpisodes = page.hasMore
                    )
                } else {
                    _uiState.value = PodcastInfoUiState.Error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = PodcastInfoUiState.Error
            }
        }
    }

    fun loadMoreEpisodes() {
        val currentState = _uiState.value
        if (currentState !is PodcastInfoUiState.Success) return
        if (currentState.isLoadingMore || !currentState.hasMoreEpisodes) return
        if (currentState.searchResults != null) return // Don't load more when searching

        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val sortParam = if (currentState.currentSort == EpisodeSort.OLDEST) "oldest" else "newest"
                val page = repository.getEpisodesPaginated(currentPodcastId, PAGE_SIZE, currentOffset, sortParam)
                currentOffset += page.episodes.size
                _uiState.value = currentState.copy(
                    episodes = currentState.episodes + page.episodes,
                    isLoadingMore = false,
                    hasMoreEpisodes = page.hasMore
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleSort() {
        val currentState = _uiState.value
        if (currentState !is PodcastInfoUiState.Success) return

        val newSort = if (currentState.currentSort == EpisodeSort.NEWEST) EpisodeSort.OLDEST else EpisodeSort.NEWEST
        currentOffset = 0
        
        _uiState.value = currentState.copy(
            currentSort = newSort,
            episodes = emptyList(),
            isLoadingMore = true,
            hasMoreEpisodes = true,
            searchQuery = "",
            searchResults = null
        )

        viewModelScope.launch {
            try {
                val sortParam = if (newSort == EpisodeSort.OLDEST) "oldest" else "newest"
                val page = repository.getEpisodesPaginated(currentPodcastId, PAGE_SIZE, 0, sortParam)
                currentOffset = page.episodes.size
                val latestState = _uiState.value as? PodcastInfoUiState.Success ?: return@launch
                _uiState.value = latestState.copy(
                    episodes = page.episodes,
                    isLoadingMore = false,
                    hasMoreEpisodes = page.hasMore
                )
            } catch (e: Exception) {
                e.printStackTrace()
                val latestState = _uiState.value as? PodcastInfoUiState.Success ?: return@launch
                _uiState.value = latestState.copy(isLoadingMore = false)
            }
        }
    }

    fun searchEpisodes(query: String) {
        val currentState = _uiState.value
        if (currentState !is PodcastInfoUiState.Success) return

        _uiState.value = currentState.copy(searchQuery = query)

        // Clear search
        if (query.isBlank()) {
            _uiState.value = currentState.copy(
                searchQuery = "",
                searchResults = null,
                isSearching = false
            )
            return
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = (_uiState.value as? PodcastInfoUiState.Success)?.copy(isSearching = true) ?: return@launch
            delay(SEARCH_DEBOUNCE_MS)
            
            try {
                // Search using user query + podcast name for better results
                val podcast = (uiState.value as? PodcastInfoUiState.Success)?.podcast
                val fullQuery = "$query ${podcast?.title}"
                
                // Use global search and filter to this podcast
                val searchResults = repository.searchPodcasts(fullQuery)
                // For now, filter client-side from loaded episodes (PI doesn't have episode search by feed)
                val latestState = _uiState.value as? PodcastInfoUiState.Success ?: return@launch
                val filtered = latestState.episodes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
                _uiState.value = latestState.copy(
                    searchResults = filtered,
                    isSearching = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                val latestState = _uiState.value as? PodcastInfoUiState.Success ?: return@launch
                _uiState.value = latestState.copy(isSearching = false, searchResults = emptyList())
            }
        }
    }

    fun toggleSubscription() {
        val currentState = _uiState.value
        if (currentState is PodcastInfoUiState.Success) {
            viewModelScope.launch {
                subscriptionRepository.toggleSubscription(currentState.podcast)
                // Refresh state
                val isSubscribed = subscriptionRepository.isSubscribed(currentState.podcast.id)
                _uiState.value = currentState.copy(isSubscribed = isSubscribed)
            }
        }
    }
}
