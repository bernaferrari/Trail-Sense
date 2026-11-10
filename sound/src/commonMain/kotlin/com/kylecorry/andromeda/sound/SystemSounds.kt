package com.kylecorry.andromeda.sound

public enum class DefaultSystemSound { Alarm, Notification, Ringtone }
public enum class SoundChannel { Media, Notification, Event, Alarm }

public interface SystemSound {
    public val isPlaying: Boolean
    public fun play()
    public fun stop()
}

/** Returns the selected sound URI, or null when unavailable. Android requires KSensor context initialization. */
public expect fun defaultSystemSoundUri(sound: DefaultSystemSound): String?

/** Opens a platform sound URI, or returns null if it cannot be played. Currently supported on Android. */
public expect fun openSystemSound(uri: String, channel: SoundChannel = SoundChannel.Media): SystemSound?
