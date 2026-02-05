package cx.aswin.boxcast.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.data.SubscriptionRepository
import cx.aswin.boxcast.core.data.PlaybackRepository
import cx.aswin.boxcast.core.data.database.ListeningHistoryEntity
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(
        val subscribedPodcasts: List<Podcast> = emptyList(),
        val likedEpisodes: List<ListeningHistoryEntity> = emptyList()
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    // Combine subscriptions and liked episodes
    val uiState: StateFlow<LibraryUiState> = combine(
        subscriptionRepository.subscribedPodcasts,
        playbackRepository.likedEpisodes
    ) { podcasts, liked ->
        LibraryUiState.Success(
            subscribedPodcasts = podcasts,
            likedEpisodes = liked
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState.Loading
        )
}
