package com.kylecorry.andromeda.widgets.tiles

import android.content.Intent
import android.app.PendingIntent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import android.app.ActivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class AndromedaTileService : TileService() {

    // Potential fix for a bug in Android where it fails to reach the IQSService
    // https://github.com/firemaples/EverTranslator/issues/130
    override fun onBind(intent: Intent?): IBinder? {
        return try {
            super.onBind(intent)
        } catch (e: Exception) {
            null
        }
    }

    fun setState(state: Int) {
        if (state != qsTile.state) {
            qsTile.state = state
            qsTile.updateTile()
        }
    }

    fun setTitle(title: String) {
        qsTile.label = title
        qsTile.updateTile()
    }

    fun setSubtitle(subtitle: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = subtitle
            qsTile.updateTile()
        }
    }

    fun setIcon(icon: Icon) {
        qsTile.icon = icon
        qsTile.updateTile()
    }

    fun setIcon(@DrawableRes icon: Int) {
        val drawable = ResourcesCompat.getDrawable(this.resources, icon, null)
        val bitmap = drawable?.toBitmap() ?: return
        val realIcon = Icon.createWithBitmap(bitmap)
        realIcon.setTint(Color.WHITE)
        setIcon(realIcon)
    }

    /** Starts foreground work from a tile click, showing a temporary activity when Android requires it. */
    fun startForegroundService(action: suspend () -> Unit) {
        val powerManager = getSystemService(PowerManager::class.java)
        val needsActivity = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !isAppForeground() &&
            powerManager?.isIgnoringBatteryOptimizations(packageName) != true
        if (!needsActivity) {
            CoroutineScope(Dispatchers.Default).launch { action() }
            return
        }

        val id = TileActionRegistry.add(action)
        val intent = Intent(this, TileActionActivity::class.java)
            .putExtra(TileActionActivity.ACTION_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startActivityAndCollapse(pending)
        } catch (error: Exception) {
            TileActionRegistry.take(id)
            throw error
        }
        CoroutineScope(Dispatchers.Default).launch {
            delay(30_000)
            TileActionRegistry.take(id)
        }
    }
}

private fun isAppForeground(): Boolean {
    val info = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(info)
    return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
        info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
}
