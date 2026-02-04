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
        val podcastGenre: String = "",
        val resumePositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val relatedEpisodes: List<Episode> = emptyList(),
        val relatedEpisodesLoading: Boolean = true
    ) : EpisodeInfoUiState
    data object Error : EpisodeInfoUiState
}

class EpisodeInfoViewModel(
    application: Application,
    private val apiBaseUrl: String,
    private val publicKey: String
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
        val currentState = _uiState.value
        // If we already have this episode loaded, don't reload
        if (currentState is EpisodeInfoUiState.Success && currentState.episode.id == episodeId) {
            return
        }

        viewModelScope.launch {
            _uiState.value = EpisodeInfoUiState.Loading
            try {
                // 1. Show immediate data (partial)
                var currentEpisode = Episode(
                    id = episodeId,
                    title = episodeTitle,
                    description = episodeDescription,
                    imageUrl = episodeImageUrl,
                    audioUrl = episodeAudioUrl,
                    duration = episodeDuration,
                    publishedDate = 0L
                )
                
                // Check for resume position immediately
                val resumeSession = playbackRepository.getSession(episodeId)
                val resumeMs = resumeSession?.positionMs ?: 0L
                val durationMs = resumeSession?.durationMs ?: (episodeDuration * 1000L)

                _uiState.value = EpisodeInfoUiState.Success(
                    episode = currentEpisode,
                    podcastId = podcastId,
                    podcastTitle = podcastTitle,
                    resumePositionMs = resumeMs,
                    durationMs = durationMs
                )
                
                // 2. Fetch full details (description, etc.)
                // Only if description is empty or we suspect it's partial? Always fetch to be safe.
                // 2. Fetch full details from Network (since we don't have local Episode table yet)
                val repository = cx.aswin.boxcast.core.data.PodcastRepository(apiBaseUrl, publicKey, getApplication())
                val fullEpisode = repository.getEpisode(episodeId)

                if (fullEpisode != null) {
                    val netImage = fullEpisode.imageUrl
                    currentEpisode = fullEpisode.copy(
                        // Preserve passed image if network one is missing
                        imageUrl = if (!netImage.isNullOrEmpty()) netImage else episodeImageUrl
                    )
                    
                    // Preserve existing relatedEpisodes state if already loaded
                    val existingState = _uiState.value as? EpisodeInfoUiState.Success
                    _uiState.value = EpisodeInfoUiState.Success(
                        episode = currentEpisode,
                        podcastId = podcastId,
                        podcastTitle = podcastTitle,
                        resumePositionMs = resumeMs,
                        durationMs = durationMs,
                        relatedEpisodes = existingState?.relatedEpisodes ?: emptyList(),
                        relatedEpisodesLoading = existingState?.relatedEpisodesLoading ?: true
                    )
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep showing partial data if success was already emitted?
                if (_uiState.value is EpisodeInfoUiState.Loading) {
                    _uiState.value = EpisodeInfoUiState.Error
                }
            }
        }
        
        // 3. Fetch related episodes AND podcast genre INDEPENDENTLY (non-blocking)
        viewModelScope.launch {
            try {
                android.util.Log.d("EpisodeInfo", "Fetching related episodes for podcastId: $podcastId")
                val repository = cx.aswin.boxcast.core.data.PodcastRepository(apiBaseUrl, publicKey, getApplication())
                
                // Fetch podcast to get genre
                val podcast = repository.getPodcastDetails(podcastId)
                val genre = podcast?.genre ?: ""
                
                // Use getEpisodesPaginated which is the correct method used elsewhere
                val page = repository.getEpisodesPaginated(podcastId, 15, 0, "newest")
                android.util.Log.d("EpisodeInfo", "Fetched ${page.episodes.size} episodes, genre: $genre")
                val relatedEps = page.episodes
                    .filter { it.id != episodeId }
                    .take(10)
                
                // Update state with related episodes and genre (only if we're in Success state)
                val currentSuccess = _uiState.value as? EpisodeInfoUiState.Success
                if (currentSuccess != null && currentSuccess.episode.id == episodeId) {
                    _uiState.value = currentSuccess.copy(
                        relatedEpisodes = relatedEps,
                        relatedEpisodesLoading = false,
                        podcastGenre = genre.ifEmpty { currentSuccess.podcastGenre }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EpisodeInfo", "Error fetching related episodes", e)
                // Mark loading as done even on failure
                val currentSuccess = _uiState.value as? EpisodeInfoUiState.Success
                if (currentSuccess != null) {
                    _uiState.value = currentSuccess.copy(relatedEpisodesLoading = false)
                }
                e.printStackTrace()
            }
        }
    }
}
