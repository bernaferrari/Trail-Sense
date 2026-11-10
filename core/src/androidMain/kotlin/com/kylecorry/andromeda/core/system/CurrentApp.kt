package com.kylecorry.andromeda.core.system

import android.app.ActivityManager
import android.content.Context

public object CurrentApp {

    public fun restart(context: Context) {
        val intent = Intents.restart(context)
        context.startActivity(intent)
        kill()
    }

    public fun kill() {
        Runtime.getRuntime().exit(0)
    }

    public fun isInForeground(includeForegroundServices: Boolean = true): Boolean {
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                (includeForegroundServices && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE)
    }

}
