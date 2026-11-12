package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.notifications.*
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import kotlin.time.Clock
import kotlin.time.Duration

class WhiteNoiseService : AndromedaService() {

    private var soundPlayer: SoundPlayer? = null
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val offTimer = CoroutineTimer {
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getAppService<Logger>().info(TAG, "Started (restarted by system: ${intent == null})")
        return tryStartForegroundOrNotify {
            super.onStartCommand(intent, flags, startId)
            isRunning = true
            ToolBroadcasts.broadcast(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED)
            val stopAt = cache.getInstant(CACHE_KEY_OFF_TIME)
            if (stopAt != null && Clock.System.now() < stopAt) {
                offTimer.once((stopAt - Clock.System.now()))
            }

            val soundId = cache.getLong(CACHE_KEY_SLEEP_SOUND_ID) ?: SleepSound.PinkNoise.id
            val sound = SleepSound.entries.withId(soundId) ?: SleepSound.PinkNoise

            soundPlayer?.fadeOff(true)
            soundPlayer = SleepSoundFactory().getSleepSound(sound)
            soundPlayer?.fadeOn()
            START_STICKY
        }
    }

    override fun getForegroundInfo(): ForegroundInfo {
        val notification = Notify.persistent(
            this,
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.tool_white_noise_title),
            getString(R.string.tap_to_turn_off),
            TrailIcons.ToolWhiteNoise,
            intent = WhiteNoiseOffReceiver.pendingIntent(this),
            showForegroundImmediate = true
        )
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        getAppService<Logger>().info(TAG, "Stopped")
        offTimer.stop()
        isRunning = false
        soundPlayer?.fadeOff(true)
        stopService(true)
        clearSleepTimer(this)
        ToolBroadcasts.broadcast(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED)
        ToolBroadcasts.broadcast(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_FINISHED)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 9874333
        private const val TAG = "WhiteNoiseService"
        const val NOTIFICATION_CHANNEL_ID = "white_noise"
        const val CACHE_KEY_OFF_TIME = WhiteNoisePlayer.CACHE_KEY_OFF_TIME

        const val CACHE_KEY_SLEEP_SOUND_ID = WhiteNoisePlayer.CACHE_KEY_SLEEP_SOUND_ID

        var isRunning = false
            private set

        fun intent(context: Context): Intent {
            return Intent(context, WhiteNoiseService::class.java)
        }

        fun play(
            context: Context,
            sleepSound: SleepSound? = null,
            duration: Duration? = null
        ) {
            val cache = DependencyRegistry.get<PreferencesSubsystem>().preferences
            if (duration != null && duration != Duration.ZERO) {
                cache.putInstant(CACHE_KEY_OFF_TIME, kotlin.time.Clock.System.now().plus(duration))
            } else {
                clearSleepTimer(context)
            }

            // Allow the last selected sound to be played
            if (sleepSound != null) {
                cache.putLong(CACHE_KEY_SLEEP_SOUND_ID, sleepSound.id)
            }
            start(context)
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, intent(context))
            } catch (e: Exception) {
                // Don't do anything
            }
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

        fun clearSleepTimer(context: Context) {
            PreferencesSubsystem.getInstance(context).preferences.remove(CACHE_KEY_OFF_TIME)
        }
    }

}
