package com.kylecorry.trail_sense.shared.views.compose

import com.kylecorry.trail_sense.shared.navigation.Route

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface TrailSenseDestinationHost {
    val lifecycleOwner: LifecycleOwner
    val lifecycleScope: CoroutineScope
    val mainActivity: MainActivity
    val intentResultRetriever: IntentResultRetriever
    /** The screen this host shows, with its arguments. */
    val route: Route

    fun requireContext(): Context
    fun getResources(): Resources
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String
    fun startActivity(intent: Intent)
    fun findNavigator(): TrailSenseNavigator
    fun toast(message: CharSequence, short: Boolean = true)
    fun inBackground(
        state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
        cancelWhenRerun: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ): Job
    fun requestCamera(action: (hasPermission: Boolean) -> Unit)
    fun requestActivityRecognition(action: (hasPermission: Boolean) -> Unit)
    fun requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit)
    fun requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit)
    fun requestBackgroundLocation(action: (hasPermission: Boolean) -> Unit)
    fun requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit)
    fun alertNoCameraPermission()
    fun alertNoActivityRecognitionPermission()
    fun showPhotoImport(
        resolution: Size?,
        useZeroShutterLag: Boolean,
        onCapture: (Uri?) -> Unit
    )

    fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        onAction: (ActionItem?) -> Unit
    )

    fun showSnackbar(
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar

    fun openWidgets()
    fun reloadTheme()
    fun refreshBeaconList()
    fun getBottomNavigationHeight(): Int
    fun addOverlayView(view: View)
    fun removeOverlayView(view: View)
}

interface TrailSenseReactiveDestinationHost : TrailSenseDestinationHost {
    val hookResetOnResume: Int
    fun cleanupReactiveEffects()
}

/** This screen's route, as the type the screen was registered for. */
inline fun <reified T : Route> TrailSenseDestinationHost.routeAs(): T = route as T
