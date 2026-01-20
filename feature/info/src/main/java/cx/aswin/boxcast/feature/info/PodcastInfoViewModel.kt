package cx.aswin.boxcast.feature.info

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface PodcastInfoUiState {
    data object Loading : PodcastInfoUiState
    data class Success(
        val podcast: Podcast,
        val episodes: List<Episode>,
        val isSubscribed: Boolean
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

    fun loadPodcast(podcastId: String) {
        viewModelScope.launch {
            _uiState.value = PodcastInfoUiState.Loading
            try {
                val podcast = repository.getPodcastDetails(podcastId)
                if (podcast != null) {
                    val episodes = repository.getEpisodes(podcastId)
                    val isSubscribed = subscriptionRepository.isSubscribed(podcastId)
                    _uiState.value = PodcastInfoUiState.Success(podcast, episodes, isSubscribed)
                } else {
                    _uiState.value = PodcastInfoUiState.Error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = PodcastInfoUiState.Error
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
