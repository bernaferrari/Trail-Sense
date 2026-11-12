package com.kylecorry.trail_sense.shared

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.shared.text.AndroidAppStrings
import com.kylecorry.trail_sense.shared.text.AndroidPlatformFormats

@SuppressLint("StaticFieldLeak")
private var instance: FormatService? = null
private val lock = Any()

/** The app's formatter, reading the app's resources and settings. */
fun FormatService.Companion.getInstance(context: Context): FormatService {
    return synchronized(lock) {
        instance ?: FormatService(
            AndroidAppStrings(context),
            AndroidPlatformFormats(context),
            UserPreferences(context.applicationContext)
        ).also { instance = it }
    }
}
