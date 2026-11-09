package com.kylecorry.andromeda.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.getSystemService

public class SystemSoundPlayer(private val context: Context) {

    private val audioManager = context.getSystemService<AudioManager>()

    public fun getAlarmUri(): Uri? {
        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
    }

    public fun getNotificationUri(): Uri? {
        return RingtoneManager.getActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_NOTIFICATION
        )
    }

    public fun getRingtoneUri(): Uri? {
        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
    }

    public fun player(soundUri: Uri, channel: AudioChannel): MediaPlayer? {
        return MediaPlayer.create(
            context,
            soundUri,
            null,
            AudioAttributes.Builder().setUsage(getUsageType(channel)).build(),
            audioManager?.generateAudioSessionId() ?: 0
        )
    }

    private fun getUsageType(channel: AudioChannel): Int {
        return when (channel) {
            AudioChannel.Media -> AudioAttributes.USAGE_MEDIA
            AudioChannel.Notification -> AudioAttributes.USAGE_NOTIFICATION
            AudioChannel.Event -> AudioAttributes.USAGE_NOTIFICATION_EVENT
            AudioChannel.Alarm -> AudioAttributes.USAGE_ALARM
        }
    }

    public enum class AudioChannel {
        Media,
        Notification,
        Event,
        Alarm,
    }

}