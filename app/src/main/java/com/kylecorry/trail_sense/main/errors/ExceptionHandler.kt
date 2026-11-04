package com.kylecorry.trail_sense.main.errors

import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.logging.Logger

object ExceptionHandler {

    fun initialize(activity: MainActivity) {
        val handler = TrailSenseExceptionHandler(activity)
        handler.bind()

        TrailSenseExceptionHandler.error?.let {
            getAppService<Logger>().error("Trail Sense", it)
            showCrashReportDialog(
                activity,
                log = it,
                onCopy = {
                    com.kylecorry.andromeda.clipboard.Clipboard.copy(activity, it, activity.getString(R.string.copied_to_clipboard_toast))
                },
                onSend = {
                    activity.startActivity(
                        Intents.email(
                            activity.getString(R.string.email),
                            "Error in ${activity.getString(R.string.app_name)}",
                            it
                        )
                    )
                }
            )
        }
        // Debug builds: `adb shell setprop debug.ts.crashpreview 1` shows the dialog with a sample log, to review it.
        if (isDebug() && TrailSenseExceptionHandler.error == null && runCatching {
                Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
                    .invoke(null, "debug.ts.crashpreview") == "1"
            }.getOrDefault(false)
        ) {
            showCrashReportDialog(activity, "java.lang.IllegalStateException: sample\n\tat com.example.Sample.run(Sample.kt:1)", onCopy = {}, onSend = {})
        }

        TrailSenseExceptionHandler.error = null
    }
}
