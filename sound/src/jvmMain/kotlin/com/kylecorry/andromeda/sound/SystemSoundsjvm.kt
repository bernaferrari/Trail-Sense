package com.kylecorry.andromeda.sound

actual fun defaultSystemSoundUri(sound: DefaultSystemSound): String? = null
actual fun openSystemSound(uri: String, channel: SoundChannel): SystemSound? = null
