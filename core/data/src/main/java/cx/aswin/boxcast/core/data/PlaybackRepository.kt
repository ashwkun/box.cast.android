package cx.aswin.boxcast.core.data

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import cx.aswin.boxcast.core.model.Episode
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.data.service.BoxCastPlaybackService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class PlaybackSession(
    val podcastId: String,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long,
    // Cached Metadata
    val episodeTitle: String,
    val podcastTitle: String,
    val imageUrl: String?, // Primary (Episode) Art
    val podcastImageUrl: String?, // Fallback (Podcast) Art
    val audioUrl: String?
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val position: Long = 0L,
    val currentEpisode: Episode? = null,
    val currentPodcast: Podcast? = null,
    val isLoading: Boolean = false
)

class PlaybackRepository(
    private val context: Context,
    private val listeningHistoryDao: cx.aswin.boxcast.core.data.database.ListeningHistoryDao
) {

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    private val _playerState = kotlinx.coroutines.flow.MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()
    
    // Scope for progress updates
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, BoxCastPlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startProgressTicker() else stopProgressTicker()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val isLoading = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                    _playerState.value = _playerState.value.copy(isLoading = isLoading)
                    
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        _playerState.value = _playerState.value.copy(isPlaying = false, position = 0)
                        stopProgressTicker()
                    }
                }
            })
            // Sync initial state
            _playerState.value = _playerState.value.copy(
                isPlaying = mediaController?.isPlaying == true,
                isLoading = mediaController?.playbackState == androidx.media3.common.Player.STATE_BUFFERING
            )
        }, MoreExecutors.directExecutor())
    }
    
    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = repositoryScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            position = controller.currentPosition,
                            duration = controller.duration.coerceAtLeast(0)
                        )
                    }
                }
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }
    
    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    suspend fun playEpisode(episode: Episode, podcast: Podcast) {
        if (mediaController == null) {
            mediaController = mediaControllerFuture?.await()
        }
        
        mediaController?.let { controller ->
            // Check if already playing this episode to avoid restart
            val isSame = _playerState.value.currentEpisode?.id == episode.id
            if (isSame) {
                controller.play()
                return@let
            }
            
            // Create MediaItem with Metadata
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(episode.title)
                .setArtist(podcast.title)
                .setArtworkUri(android.net.Uri.parse(episode.imageUrl ?: podcast.imageUrl))
                .setDisplayTitle(episode.title)
                .setSubtitle(podcast.title)
                .build()
                
            val mediaItem = MediaItem.Builder()
                .setUri(episode.audioUrl)
                .setMediaMetadata(metadata)
                .build()
                
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
            
            // Update local state immediately with metadata
            _playerState.value = _playerState.value.copy(
                currentEpisode = episode,
                currentPodcast = podcast,
                isPlaying = true,
                position = 0,
                duration = episode.duration.toLong() * 1000
            )

            // Save initial state to DB
            savePlaybackState(
                podcastId = podcast.id,
                episodeId = episode.id,
                positionMs = 0,
                durationMs = episode.duration.toLong() * 1000,
                episodeTitle = episode.title,
                episodeImageUrl = episode.imageUrl,
                podcastImageUrl = podcast.imageUrl,
                episodeAudioUrl = episode.audioUrl,
                podcastName = podcast.title,
                isCompleted = false
            )
        }
    }
    
    fun pause() {
        mediaController?.pause()
    }
    
    fun resume() {
        mediaController?.play()
    }
    
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(position = positionMs)
    }
    
    fun skipForward() {
        seekTo((_playerState.value.position + 30000).coerceAtMost(_playerState.value.duration))
    }
    
    fun skipBackward() {
        seekTo((_playerState.value.position - 10000).coerceAtLeast(0))
    }

    val lastPlayedSession: Flow<PlaybackSession?> = listeningHistoryDao.getResumeItems()
        .map { historyList ->
            val latest = historyList.firstOrNull()
            if (latest != null) {
                PlaybackSession(
                    podcastId = latest.podcastId,
                    episodeId = latest.episodeId,
                    positionMs = latest.progressMs,
                    durationMs = latest.durationMs,
                    timestamp = latest.lastPlayedAt,
                    episodeTitle = latest.episodeTitle,
                    podcastTitle = latest.podcastName,
                    imageUrl = latest.episodeImageUrl,
                    podcastImageUrl = latest.podcastImageUrl,
                    audioUrl = latest.episodeAudioUrl
                )
            } else {
                null
            }
        }

    val resumeSessions: Flow<List<PlaybackSession>> = listeningHistoryDao.getResumeItems()
        .map { historyList ->
            historyList.map { entity ->
                PlaybackSession(
                    podcastId = entity.podcastId,
                    episodeId = entity.episodeId,
                    positionMs = entity.progressMs,
                    durationMs = entity.durationMs,
                    timestamp = entity.lastPlayedAt,
                    episodeTitle = entity.episodeTitle,
                    podcastTitle = entity.podcastName,
                    imageUrl = entity.episodeImageUrl,
                    podcastImageUrl = entity.podcastImageUrl,
                    audioUrl = entity.episodeAudioUrl
                )
            }
        }

    fun getAllHistory(): Flow<List<cx.aswin.boxcast.core.data.database.ListeningHistoryEntity>> {
        return listeningHistoryDao.getAllHistory()
    }

    suspend fun savePlaybackState(
        podcastId: String, 
        episodeId: String, 
        positionMs: Long, 
        durationMs: Long,
        // New params for cache
        episodeTitle: String,
        episodeImageUrl: String?,
        podcastImageUrl: String?,
        episodeAudioUrl: String,
        podcastName: String,
        isCompleted: Boolean
    ) {
        val entity = cx.aswin.boxcast.core.data.database.ListeningHistoryEntity(
            episodeId = episodeId,
            podcastId = podcastId,
            episodeTitle = episodeTitle,
            episodeImageUrl = episodeImageUrl,
            podcastImageUrl = podcastImageUrl,
            episodeAudioUrl = episodeAudioUrl,
            podcastName = podcastName,
            progressMs = positionMs,
            durationMs = durationMs,
            isCompleted = isCompleted,
            lastPlayedAt = System.currentTimeMillis(),
            isDirty = true
        )
        listeningHistoryDao.upsert(entity)
    }
    suspend fun deleteSession(episodeId: String) {
        listeningHistoryDao.delete(episodeId)
    }

    suspend fun getSession(episodeId: String): PlaybackSession? {
        val entity = listeningHistoryDao.getHistoryItem(episodeId) ?: return null
        return PlaybackSession(
            podcastId = entity.podcastId,
            episodeId = entity.episodeId,
            positionMs = entity.progressMs,
            durationMs = entity.durationMs,
            timestamp = entity.lastPlayedAt,
            episodeTitle = entity.episodeTitle,
            podcastTitle = entity.podcastName,
            imageUrl = entity.episodeImageUrl,
            podcastImageUrl = entity.podcastImageUrl,
            audioUrl = entity.episodeAudioUrl
        )
    }
}
