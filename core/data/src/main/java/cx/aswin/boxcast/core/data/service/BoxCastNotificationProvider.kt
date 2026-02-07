package cx.aswin.boxcast.core.data.service

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import cx.aswin.boxcast.core.data.R

@UnstableApi

class BoxCastNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val player = session.player
        val buttons = mutableListOf<CommandButton>()

        // 1. Seek Backward (10s)
        buttons.add(
            CommandButton.Builder()
                .setDisplayName("Rewind 10s")
                .setIconResId(cx.aswin.boxcast.core.designsystem.R.drawable.rounded_replay_10_24)
                .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                .build()
        )

        // 2. Play/Pause
        if (showPauseButton && player.getPlayWhenReady()) { 
             buttons.add(
                CommandButton.Builder()
                    .setDisplayName("Pause")
                    .setIconResId(androidx.media3.ui.R.drawable.exo_icon_pause)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .build()
            )
        } else {
             buttons.add(
                CommandButton.Builder()
                    .setDisplayName("Play")
                    .setIconResId(androidx.media3.ui.R.drawable.exo_icon_play)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .build()
            )
        }

        // 3. Seek Forward (30s)
        buttons.add(
            CommandButton.Builder()
                .setDisplayName("Forward 30s")
                .setIconResId(cx.aswin.boxcast.core.designsystem.R.drawable.rounded_forward_30_24)
                .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                .build()
        )

        return ImmutableList.copyOf(buttons)
    }
    
    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        // We want 0, 1, 2 (Rewind, Play, Forward) in compact view
        // The indices passed to setShowActionsInCompactView refer to the indices in the actions added to the builder.
        // Default impl adds all mediaButtons to builder.
        // So we just return the indices we want.
        
        // Let super add the actions? No, super implementation uses getMediaButtons and adds them.
        // Wait, if we override getMediaButtons, does super.addNotificationActions use it? Yes.
        // But we need to define which ones are compact.
        // The signature `addNotificationActions` returns `int[]` which are the compact view indices.
        
        // We must call super to add actions to builder?
        // Or we replicate logic?
        // Default provider documentation says:
        // "Override getMediaButtons to customize buttons."
        // "By default... seekPrev, Play/Pause, seekNext in compact view."
        // "This can be customized by defining the index of the command in compact view... COMMAND_KEY_COMPACT_VIEW_INDEX"
        
        // So we can set extras on the commands in `getMediaButtons`!
        
        return super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
    }
}
