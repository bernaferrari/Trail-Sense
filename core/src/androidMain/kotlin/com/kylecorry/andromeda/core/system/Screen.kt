package com.kylecorry.andromeda.core.system

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.Size
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService

public object Screen {

    public fun setKeepScreenOn(window: Window, keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    public fun setBrightness(window: Window, brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0f, 1f)
        window.attributes = layoutParams
    }

    public fun resetBrightness(window: Window) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    public fun setAllowScreenshots(window: Window, allowed: Boolean) {
        if (allowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }





    public fun dpi(context: Context): Float {
        return context.resources.displayMetrics.densityDpi.toFloat()
    }

    public fun ydpi(context: Context): Float {
        return context.resources.displayMetrics.ydpi
    }

    public fun xdpi(context: Context): Float {
        return context.resources.displayMetrics.xdpi
    }

    public fun density(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    @Suppress("DEPRECATION")
    public fun setShowWhenLocked(activity: Activity, showWhenLocked: Boolean) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                activity.setShowWhenLocked(showWhenLocked)
            }
            showWhenLocked -> {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
            else -> {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }

    public fun getWindowHeight(context: Context): Int {
        return getWindowSize(context).height
    }

    public fun getWindowWidth(context: Context): Int {
        return getWindowSize(context).width
    }

    public fun getWindowSize(context: Context): Size {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindowSizeSDK30(context)
        } else {
            getWindowSizeLegacy(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun getWindowSizeLegacy(context: Context): Size {
        val window = context.getSystemService<WindowManager>()!!
        val point = Point()
        window.defaultDisplay.getSize(point)
        return Size(point.x, point.y)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getWindowSizeSDK30(context: Context): Size {
        val window = context.getSystemService<WindowManager>()!!
        val metrics = window.currentWindowMetrics
        val windowInsets = metrics.windowInsets
        val insets = windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.navigationBars()
                    or WindowInsets.Type.displayCutout()
        )

        val insetsWidth = insets.right + insets.left
        val insetsHeight = insets.top + insets.bottom
        val bounds = metrics.bounds
        return Size(
            bounds.width() - insetsWidth,
            bounds.height() - insetsHeight
        )
    }
}