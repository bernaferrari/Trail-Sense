package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.content.Context
import android.content.Intent
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class FlashlightToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val flashlight = FlashlightSubsystem.getInstance(context)
        val isOn = flashlight.getMode() != FlashlightMode.Off

        val intent = if (isForegroundServiceWorkaroundNeeded(context)) {
            Intent(context, FlashlightWidgetActivityWorkaround::class.java)
        } else {
            Intent(context, FlashlightWidgetReceiver::class.java)
        }

        return ToolWidgetContent.Simple(
            title = null,
            subtitle = if (isOn) context.getString(R.string.on) else context.getString(R.string.off),
            icon = R.drawable.flashlight,
            showTitle = false,
            clickAction = if (isForegroundServiceWorkaroundNeeded(context)) {
                WidgetClickAction.StartActivity(intent)
            } else {
                WidgetClickAction.SendBroadcast(intent)
            }
        )
    }

    private fun isForegroundServiceWorkaroundNeeded(context: Context): Boolean {
        if ((context as? MainActivity) != null) {
            return false
        }

        // The bug only happens on Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        // Normally we would check if the app is in the foreground, but widgets don't initialize often

        return !Permissions.isIgnoringBatteryOptimizations(context)
    }
}
