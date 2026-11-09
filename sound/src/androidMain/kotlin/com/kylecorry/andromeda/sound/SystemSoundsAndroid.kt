package com.kylecorry.andromeda.sound

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import com.ksensor.core.context.KSensorContext

actual fun defaultSystemSoundUri(sound: DefaultSystemSound): String? {
    val type = when (sound) {
        DefaultSystemSound.Alarm -> RingtoneManager.TYPE_ALARM
        DefaultSystemSound.Notification -> RingtoneManager.TYPE_NOTIFICATION
        DefaultSystemSound.Ringtone -> RingtoneManager.TYPE_RINGTONE
    }
    return RingtoneManager.getActualDefaultRingtoneUri(KSensorContext.get(), type)?.toString()
}

actual fun openSystemSound(uri: String, channel: SoundChannel): SystemSound? {
    val sound = RingtoneManager.getRingtone(KSensorContext.get(), Uri.parse(uri)) ?: return null
    val usage = when (channel) {
        SoundChannel.Media -> AudioAttributes.USAGE_MEDIA
        SoundChannel.Notification -> AudioAttributes.USAGE_NOTIFICATION
        SoundChannel.Event -> AudioAttributes.USAGE_NOTIFICATION_EVENT
        SoundChannel.Alarm -> AudioAttributes.USAGE_ALARM
    }
    sound.audioAttributes = AudioAttributes.Builder().setUsage(usage).build()
    return object : SystemSound {
        override val isPlaying: Boolean get() = sound.isPlaying
        override fun play() = sound.play()
        override fun stop() = sound.stop()
    }
}
