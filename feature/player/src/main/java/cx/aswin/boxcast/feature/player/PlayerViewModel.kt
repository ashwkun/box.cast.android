package cx.aswin.boxcast.feature.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cx.aswin.boxcast.core.data.PodcastRepository
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(
        val podcast: Podcast,
        val episodes: List<Episode>,
        val currentEpisode: Episode? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : PlayerUiState
    data object Error : PlayerUiState
}

class PlayerViewModel(
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
    private val playbackRepository = cx.aswin.boxcast.core.data.PlaybackRepository(application, database.listeningHistoryDao())
    
    // START: Playback State Mapping
    // We combine the Repository State + Local UI State (playlist info)
    
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Observe Playback Repository State
        viewModelScope.launch {
            playbackRepository.playerState.collect { playerState ->
                 val currentUi = _uiState.value
                 if (currentUi is PlayerUiState.Success) {
                     // Sync player state to UI
                     // Note: We might want to trust the Repository's current episode if it matches the podcast context
                     val syncedEpisode = playerState.currentEpisode ?: currentUi.currentEpisode
                     
                     _uiState.value = currentUi.copy(
                         currentEpisode = syncedEpisode,
                         isPlaying = playerState.isPlaying,
                         positionMs = playerState.position,
                         durationMs = playerState.duration
                     )
                 }
            }
        }
    }

    fun loadPodcast(podcastId: String) {
        viewModelScope.launch {
            if (_uiState.value is PlayerUiState.Success) return@launch // Already loaded?
            
            _uiState.value = PlayerUiState.Loading
            try {
                // Fetch podcast details and episodes using feed ID
                val podcast = repository.getPodcastDetails(podcastId)
                
                if (podcast != null) {
                     val episodes = repository.getEpisodes(podcastId)
                     
                     // Check if global player is already playing something from this podcast
                     val globalState = playbackRepository.playerState.value
                     val isSamePodcast = globalState.currentPodcast?.id == podcastId
                     
                     val initialEpisode = if (isSamePodcast) globalState.currentEpisode else episodes.firstOrNull()
                     
                     _uiState.value = PlayerUiState.Success(
                         podcast = podcast, 
                         episodes = episodes,
                         currentEpisode = initialEpisode
                     )
                     
                     // Auto-play first episode if available and NOT already playing this podcast
                     if (episodes.isNotEmpty() && !isSamePodcast) {
                         playEpisode(episodes.first())
                     }
                } else {
                    _uiState.value = PlayerUiState.Error
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error
                e.printStackTrace()
            }
        }
    }

    fun playEpisode(episode: Episode) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            viewModelScope.launch {
                playbackRepository.playEpisode(episode, currentState.podcast)
            }
        }
    }
    
    fun togglePlayPause() {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Success) {
            if (currentState.isPlaying) {
                playbackRepository.pause()
            } else {
                playbackRepository.resume()
            }
        }
    }
    
    fun seekTo(positionMs: Long) {
        playbackRepository.seekTo(positionMs)
    }
    
    fun skipForward() {
        playbackRepository.skipForward()
    }
    
    fun skipBackward() {
        playbackRepository.skipBackward()
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT release player here, it's global service
    }
}
