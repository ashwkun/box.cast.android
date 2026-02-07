package cx.aswin.boxcast.core.data.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

class BoxCastPlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        
        // Configure AudioAttributes for Focus and Background Playback
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        // Configure CacheDataSource for Offline Playback
        val cache = cx.aswin.boxcast.core.data.DownloadRepository.getCache(this)
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(androidx.media3.common.util.Util.getUserAgent(this, "BoxCast"))
            .setAllowCrossProtocolRedirects(true)
            
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)
            
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true) // Handle Audio Focus
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK) // Prevent CPU sleep during streaming
            .setHandleAudioBecomingNoisy(true) // Pause on headphone disconnect
            .build()

        val intent = Intent()
        intent.component = android.content.ComponentName("cx.aswin.boxcast", "cx.aswin.boxcast.MainActivity")
        intent.putExtra("EXTRA_OPEN_PLAYER", true) // Notification Click -> Open Player
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    // Callback to handle custom commands or library browsing if needed
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        // Implement callback overrides here if needed
    }
}
