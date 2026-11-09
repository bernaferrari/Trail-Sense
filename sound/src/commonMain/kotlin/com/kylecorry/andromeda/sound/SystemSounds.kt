package com.kylecorry.andromeda.sound

enum class DefaultSystemSound { Alarm, Notification, Ringtone }
enum class SoundChannel { Media, Notification, Event, Alarm }

interface SystemSound {
    val isPlaying: Boolean
    fun play()
    fun stop()
}

/** Returns the selected sound URI, or null when unavailable. Android requires KSensor context initialization. */
expect fun defaultSystemSoundUri(sound: DefaultSystemSound): String?

/** Opens a platform sound URI, or returns null if it cannot be played. Currently supported on Android. */
expect fun openSystemSound(uri: String, channel: SoundChannel = SoundChannel.Media): SystemSound?
