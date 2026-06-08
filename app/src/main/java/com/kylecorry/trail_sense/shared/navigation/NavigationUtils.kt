package com.kylecorry.trail_sense.shared.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUiSaveStateControl
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NavigationUtils {

    fun pendingIntent(context: Context, @IdRes action: Int, args: Bundle? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = "com.kylecorry.trail_sense.NAVIGATE"
            putExtra("destination", action)
            if (args != null) {
                putExtra("arguments", args)
            }
        }
        return PendingIntent.getActivity(
            context,
            action,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun toolPendingIntent(context: Context, toolId: Long, args: Bundle? = null): PendingIntent {
        val tool = Tools.getTool(context, toolId)!!
        return pendingIntent(context, tool.navAction, args)
    }

    @OptIn(NavigationUiSaveStateControl::class)
    fun BottomNavigationView.setupWithNavController(
        navController: NavController,
        saveState: Boolean
    ) {
        NavigationUI.setupWithNavController(this, navController, saveState)
    }

}
