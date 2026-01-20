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
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).build()
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
