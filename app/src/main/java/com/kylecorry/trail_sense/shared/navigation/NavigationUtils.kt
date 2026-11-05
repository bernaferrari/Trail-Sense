package com.kylecorry.trail_sense.shared.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/** Intents that open a screen from outside the app's UI: notifications, widgets, shortcuts and tiles. */
object NavigationUtils {

    const val ACTION_NAVIGATE = "com.kylecorry.trail_sense.NAVIGATE"
    const val EXTRA_ROUTE = "route"

    fun pendingIntent(context: Context, route: Route, requestCode: Int = route.hashCode()): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent(context, route),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun intent(context: Context, route: Route): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_NAVIGATE
            putExtra(EXTRA_ROUTE, TrailSenseNavigator.encode(route))
        }
    }

    fun toolPendingIntent(context: Context, toolId: Long): PendingIntent {
        return pendingIntent(context, Tools.getTool(context, toolId)!!.route)
    }

    fun toolIntent(context: Context, toolId: Long): Intent {
        return intent(context, Tools.getTool(context, toolId)!!.route)
    }

    /** The route an intent from [intent] asks for, or null when it isn't a navigation intent. */
    fun routeFrom(intent: Intent): Route? {
        if (intent.action != ACTION_NAVIGATE) return null
        return TrailSenseNavigator.decode(intent.getStringExtra(EXTRA_ROUTE))
    }
}
