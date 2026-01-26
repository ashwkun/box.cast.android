package cx.aswin.boxcast.core.data

import android.util.Log

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
    val bufferedPosition: Long = 0L,
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
    
    // Preferences for session state
    private val prefs = context.getSharedPreferences("boxcast_player", Context.MODE_PRIVATE)
    private val KEY_PLAYER_DISMISSED = "player_dismissed"
    
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
                    Log.d("PlaybackRepo", "onIsPlayingChanged: isPlaying=$isPlaying, currentPos=${mediaController?.currentPosition}")
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressTicker()
                    } else {
                        stopProgressTicker()
                        // Save state when paused - delay to prevent list reorder during pod switching
                        repositoryScope.launch { 
                            kotlinx.coroutines.delay(10000) // 10 second delay
                            saveCurrentState() 
                        }
                    }
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
            
            // Sync state from MediaController (handles app coming back from background)
            syncStateFromMediaController()
        }, MoreExecutors.directExecutor())
    }
    
    /**
     * Sync playback state from the MediaController.
     * Called when MediaController connects (including when app comes back from background).
     */
    private fun syncStateFromMediaController() {
        val controller = mediaController ?: return
        
        val isPlaying = controller.isPlaying
        val isLoading = controller.playbackState == androidx.media3.common.Player.STATE_BUFFERING
        val currentPosition = controller.currentPosition.coerceAtLeast(0)
        val bufferedPosition = controller.bufferedPosition.coerceAtLeast(0)
        val duration = controller.duration.coerceAtLeast(0)
        val hasMedia = controller.mediaItemCount > 0
        
        if (hasMedia && _playerState.value.currentEpisode == null) {
            // MediaController has media but we don't have metadata - restore from DB
            repositoryScope.launch {
                val lastSession = listeningHistoryDao.getLastPlayedSession()
                if (lastSession != null) {
                    val episode = Episode(
                        id = lastSession.episodeId,
                        title = lastSession.episodeTitle,
                        description = "",
                        audioUrl = lastSession.episodeAudioUrl ?: "",
                        imageUrl = lastSession.episodeImageUrl,
                        duration = (lastSession.durationMs / 1000).toInt(),
                        publishedDate = 0L
                    )
                    val podcast = Podcast(
                        id = lastSession.podcastId,
                        title = lastSession.podcastName,
                        artist = "",
                        imageUrl = lastSession.podcastImageUrl ?: "",
                        description = null,
                        genre = "Podcast"
                    )
                    _playerState.value = PlayerState(
                        currentEpisode = episode,
                        currentPodcast = podcast,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        position = currentPosition,
                        bufferedPosition = bufferedPosition,
                        duration = if (duration > 0) duration else lastSession.durationMs
                    )
                    if (isPlaying) startProgressTicker()
                }
            }
        } else {
            // Just sync playback state
            _playerState.value = _playerState.value.copy(
                isPlaying = isPlaying,
                isLoading = isLoading,
                position = if (currentPosition > 0) currentPosition else _playerState.value.position,
                bufferedPosition = bufferedPosition,
                duration = if (duration > 0) duration else _playerState.value.duration
            )
            if (isPlaying) startProgressTicker()
        }
    }
    
    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = repositoryScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying || controller.isLoading) {
                        val currentPos = controller.currentPosition
                        val bufferedPos = controller.bufferedPosition
                        val currentDur = controller.duration.coerceAtLeast(0)
                        
                        _playerState.value = _playerState.value.copy(
                            position = currentPos,
                            bufferedPosition = bufferedPos,
                            duration = currentDur
                        )
                        
                        // Save progress periodically (every ~10 seconds)
                        if (System.currentTimeMillis() % 10000 < 500) {
                             saveCurrentState()
                        }
                    }
                }
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }
    
    // Helper to save current state
    private suspend fun saveCurrentState() {
        val state = _playerState.value
        val episode = state.currentEpisode ?: return
        val podcast = state.currentPodcast ?: return
        
        savePlaybackState(
            podcastId = podcast.id,
            episodeId = episode.id,
            positionMs = state.position,
            durationMs = state.duration,
            episodeTitle = episode.title,
            episodeImageUrl = episode.imageUrl,
            podcastImageUrl = podcast.imageUrl,
            episodeAudioUrl = episode.audioUrl,
            podcastName = podcast.title,
            isCompleted = false
        )
    }
    
    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    suspend fun playEpisode(episode: Episode, podcast: Podcast) {
        Log.d("PlaybackRepo", "playEpisode() called: episode=${episode.title} (id=${episode.id}), podcast=${podcast.title}")
        
        // Reset dismissed flag - user is actively playing
        prefs.edit().putBoolean(KEY_PLAYER_DISMISSED, false).apply()
        
        if (mediaController == null) {
            mediaController = mediaControllerFuture?.await()
        }
        
        mediaController?.let { controller ->
            // Check if already playing this episode
            val isSame = _playerState.value.currentEpisode?.id == episode.id
            // Only short-circuit if we actually have the media loaded
            if (isSame && controller.mediaItemCount > 0) {
                // If same episode and loaded, just resume
                if (!controller.isPlaying) {
                    controller.play()
                }
                return@let
            }
            
            // Check for saved progress for this episode
            var startPosition = 0L
            val savedProgress = listeningHistoryDao.getHistoryItem(episode.id)
            if (savedProgress != null && !savedProgress.isCompleted) {
                startPosition = savedProgress.progressMs
            }
            
            // Fallback: If we already have this episode loaded in memory (e.g. from restoreLastSession),
            // and it has a valid position, use that if it's better than DB result.
            if (_playerState.value.currentEpisode?.id == episode.id) {
                if (_playerState.value.position > startPosition) {
                    startPosition = _playerState.value.position
                }
            }
            
            // Create MediaItem with Metadata
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(episode.title)
                .setArtist(podcast.title)
                .setArtworkUri(android.net.Uri.parse(episode.imageUrl?.takeIf { it.isNotBlank() } ?: podcast.imageUrl))
                .setDisplayTitle(episode.title)
                .setSubtitle(podcast.title)
                .build()
           
            val mediaItem = MediaItem.Builder()
                .setUri(episode.audioUrl)
                .setMediaMetadata(metadata)
                .build()
            
            // Use setMediaItem with start position to avoid race condition
            controller.setMediaItem(mediaItem, startPosition.coerceAtLeast(0L))
            controller.prepare()
            controller.play()
            
            // Update local state immediately with metadata
            val durationMs = if (savedProgress != null && savedProgress.durationMs > 0) {
                savedProgress.durationMs
            } else {
                episode.duration.toLong() * 1000
            }
            
            _playerState.value = _playerState.value.copy(
                currentEpisode = episode,
                currentPodcast = podcast,
                isPlaying = true,
                position = startPosition,
                duration = durationMs
            )
            
            // Note: We intentionally don't save to DB here to prevent list reordering during click
            // Progress is saved on: pause, seek, and periodic ticker (every 10s)
        }
    }
    
    /**
     * Restore the last played session on app startup (does NOT auto-play)
     */
    suspend fun restoreLastSession(): Boolean {
        // Don't restore if player was explicitly dismissed
        if (prefs.getBoolean(KEY_PLAYER_DISMISSED, false)) {
            return false
        }
        
        val lastSession = listeningHistoryDao.getLastPlayedSession() ?: return false
        
        // Construct Episode and Podcast from cached data
        val episode = Episode(
            id = lastSession.episodeId,
            title = lastSession.episodeTitle,
            description = "",
            audioUrl = lastSession.episodeAudioUrl ?: return false,
            imageUrl = lastSession.episodeImageUrl,
            duration = (lastSession.durationMs / 1000).toInt(),
            publishedDate = 0L
        )
        
        val podcast = Podcast(
            id = lastSession.podcastId,
            title = lastSession.podcastName,
            artist = "",
            imageUrl = lastSession.podcastImageUrl ?: "",
            description = null,
            genre = "Podcast"
        )
        
        // Update state but don't play
        _playerState.value = _playerState.value.copy(
            currentEpisode = episode,
            currentPodcast = podcast,
            isPlaying = false,
            position = lastSession.progressMs,
            duration = lastSession.durationMs
        )
        return true
    }
    
    /**
     * Clear the current session (for swipe-to-dismiss)
     */
    fun clearSession() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        stopProgressTicker()
        _playerState.value = PlayerState()
        // Mark as dismissed so we don't restore on next app launch
        prefs.edit().putBoolean(KEY_PLAYER_DISMISSED, true).apply()
    }
    
    fun pause() {
        mediaController?.pause()
    }
    
    fun resume() {
        val controller = mediaController ?: return
        
        Log.d("PlaybackRepo", "resume() called: mediaItemCount=${controller.mediaItemCount}, statePos=${_playerState.value.position}")
        
        // If controller has no media but we have state, load the episode first
        if (controller.mediaItemCount == 0 && _playerState.value.currentEpisode != null) {
            val episode = _playerState.value.currentEpisode!!
            val podcast = _playerState.value.currentPodcast
            val savedPosition = _playerState.value.position
            
            Log.d("PlaybackRepo", "resume(): Loading media with savedPosition=$savedPosition")
            
            repositoryScope.launch {
                // Build and load the media
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(podcast?.title ?: "")
                    .setArtworkUri(android.net.Uri.parse(episode.imageUrl?.takeIf { it.isNotBlank() } ?: podcast?.imageUrl ?: ""))
                    .setDisplayTitle(episode.title)
                    .setSubtitle(podcast?.title ?: "")
                    .build()
                
                val mediaItem = MediaItem.Builder()
                    .setUri(episode.audioUrl)
                    .setMediaMetadata(metadata)
                    .build()
                
                // Use setMediaItem with start position to avoid race condition
                // This ensures the player starts at the correct position atomically
                Log.d("PlaybackRepo", "resume(): Calling setMediaItem with position=$savedPosition")
                controller.setMediaItem(mediaItem, savedPosition.coerceAtLeast(0L))
                controller.prepare()
                Log.d("PlaybackRepo", "resume(): After prepare, controller.currentPosition=${controller.currentPosition}")
                controller.play()
            }
        } else {
            Log.d("PlaybackRepo", "resume(): Media exists, just calling play()")
            controller.play()
        }
    }
    
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(position = positionMs)
        
        // Save state on seek
        repositoryScope.launch { saveCurrentState() }
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
