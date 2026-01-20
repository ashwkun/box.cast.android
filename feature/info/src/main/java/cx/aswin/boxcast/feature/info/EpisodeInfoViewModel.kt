package cx.aswin.boxcast.feature.info

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cx.aswin.boxcast.core.model.Episode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface EpisodeInfoUiState {
    data object Loading : EpisodeInfoUiState
    data class Success(
        val episode: Episode,
        val podcastId: String,
        val podcastTitle: String,
        val resumePositionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : EpisodeInfoUiState
    data object Error : EpisodeInfoUiState
}

class EpisodeInfoViewModel(
    application: Application,
    private val apiBaseUrl: String,
    private val apiKey: String
) : AndroidViewModel(application) {

    private val database = cx.aswin.boxcast.core.data.database.BoxCastDatabase.getDatabase(application)
    private val playbackRepository = cx.aswin.boxcast.core.data.PlaybackRepository(application, database.listeningHistoryDao())

    private val _uiState = MutableStateFlow<EpisodeInfoUiState>(EpisodeInfoUiState.Loading)
    val uiState: StateFlow<EpisodeInfoUiState> = _uiState.asStateFlow()

    fun loadEpisode(
        episodeId: String,
        episodeTitle: String,
        episodeDescription: String,
        episodeImageUrl: String,
        episodeAudioUrl: String,
        episodeDuration: Int,
        podcastId: String,
        podcastTitle: String
    ) {
        viewModelScope.launch {
            _uiState.value = EpisodeInfoUiState.Loading
            try {
                // Build Episode from passed data
                val episode = Episode(
                    id = episodeId,
                    title = episodeTitle,
                    description = episodeDescription,
                    imageUrl = episodeImageUrl,
                    audioUrl = episodeAudioUrl,
                    duration = episodeDuration,
                    publishedDate = 0L
                )
                
                // Check for resume position
                val resumeSession = playbackRepository.getSession(episodeId)
                val resumeMs = resumeSession?.positionMs ?: 0L
                val durationMs = resumeSession?.durationMs ?: (episodeDuration * 1000L)

                _uiState.value = EpisodeInfoUiState.Success(
                    episode = episode,
                    podcastId = podcastId,
                    podcastTitle = podcastTitle,
                    resumePositionMs = resumeMs,
                    durationMs = durationMs
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = EpisodeInfoUiState.Error
            }
        }
    }
}
