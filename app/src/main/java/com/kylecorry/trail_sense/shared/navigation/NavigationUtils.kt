package com.kylecorry.trail_sense.shared.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NavigationUtils {

    fun pendingIntent(context: Context, @IdRes action: Int, args: Bundle? = null): PendingIntent {
        val intent = intent(context, action, args)
        return PendingIntent.getActivity(
            context,
            action,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun intent(context: Context, @IdRes action: Int, args: Bundle? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            this.action = "com.kylecorry.trail_sense.NAVIGATE"
            putExtra("destination", action)
            if (args != null) {
                putExtra("arguments", args)
            }
        }
    }

    fun toolPendingIntent(context: Context, toolId: Long, args: Bundle? = null): PendingIntent {
        val tool = Tools.getTool(context, toolId)!!
        return pendingIntent(context, tool.navAction, args)
    }

    fun toolIntent(context: Context, toolId: Long, args: Bundle? = null): Intent {
        val tool = Tools.getTool(context, toolId)!!
        return intent(context, tool.navAction, args)
    }

}
