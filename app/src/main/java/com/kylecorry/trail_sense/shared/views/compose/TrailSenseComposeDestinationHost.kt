package com.kylecorry.trail_sense.shared.views.compose

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.camera.AndroidCamera
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.PermissionRationale
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.camera.PhotoImportDialog
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForgroundService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.ActionSheetContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class TrailSenseComposeDestinationHost(
    override val context: Context,
    private val activity: MainActivity,
    override val route: Route,
    override val lifecycleOwner: LifecycleOwner,
    private val requestRuntimePermissions: (Array<String>, () -> Unit) -> Unit
) : AndroidDestinationHost, ScreenEffects {

    override val navigator: TrailSenseNavigator
        get() = activity.navigator ?: error("The app navigator is not available")

    private var actionSheet by mutableStateOf<ActionSheetState?>(null)
    private var photoImportDialog by mutableStateOf<PhotoImportDialog?>(null)
    override var screenController: Any? = null
    private var specialPermissionAction: (() -> Unit)? = null

    private val lifecycleScope: CoroutineScope
        get() = lifecycleOwner.lifecycleScope

    private fun getString(resId: Int, vararg formatArgs: Any?): String = context.getString(resId, *formatArgs)

    private fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        stats: List<TrailSenseStat> = emptyList(),
        onAction: (ActionItem?) -> Unit
    ) {
        actionSheet = ActionSheetState(title, subtitle, actions, stats, onAction)
    }

    private fun showPhotoImport(
        resolution: android.util.Size?,
        useZeroShutterLag: Boolean,
        onCapture: (android.net.Uri?) -> Unit
    ) {
        photoImportDialog = PhotoImportDialog(
            context,
            lifecycleOwner,
            lifecycleScope,
            resolution,
            useZeroShutterLag
        ) {
            photoImportDialog = null
            onCapture(it)
        }.also {
            it.show()
        }
    }

    private fun requestCamera(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            action(AndroidCamera.isAvailable(context))
        }
    }

    private fun requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
                action(Permissions.canRecognizeActivity(context))
            }
        } else {
            action(true)
        }
    }

    private fun requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit) {
        requestPermission(
            SpecialPermission.SCHEDULE_EXACT_ALARMS,
            PermissionRationale(
                getString(R.string.allow_schedule_exact_alarms, getString(R.string.app_name)),
                DependencyRegistry.get<MarkdownService>().toMarkdown(
                    getString(
                        R.string.allow_schedule_exact_alarms_instructions,
                        getString(R.string.app_name),
                        getString(R.string.settings)
                    )
                ),
                ok = getString(R.string.settings),
            )
        ) {
            val isGranted = Permissions.hasPermission(
                context,
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
            if (!isGranted) {
                Alerts.snackbar(
                    context,
                    getString(R.string.exact_alarm_permission_denied),
                    short = false
                )
            }
            action(isGranted)
        }
    }

    private fun requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit) {
        if (Permissions.canStartLocationForgroundService(context)) {
            action(true)
            return
        }

        requestPermissions(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            val hasPermission = Permissions.canStartLocationForgroundService(context)
            if (!hasPermission) {
                Alerts.snackbar(context, getString(R.string.backtrack_no_permission))
            }
            action(hasPermission)
        }
    }

    private fun requestBackgroundLocation(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            action(Permissions.isBackgroundLocationEnabled(context))
        }
    }

    private fun requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit) {
        requestPermission(
            SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS,
            PermissionRationale(
                getString(
                    R.string.allow_ignore_battery_restrictions,
                    getString(R.string.app_name)
                ),
                DependencyRegistry.get<MarkdownService>().toMarkdown(
                    getString(
                        R.string.allow_ignore_battery_restrictions_instructions,
                        getString(R.string.settings)
                    )
                ),
                ok = getString(R.string.settings),
            )
        ) {
            val isGranted = Permissions.hasPermission(
                context,
                SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS
            )
            if (!isGranted) {
                message(getString(R.string.battery_usage_restricted), short = false)
            }
            action(isGranted)
        }
    }

    private fun message(message: CharSequence, short: Boolean) {
        Alerts.snackbar(context, message, short)
    }

    private fun inBackground(
        state: BackgroundMinimumState,
        cancelWhenRerun: Boolean,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return lifecycleOwner.inBackground(state, cancelWhenRerun = cancelWhenRerun, block = block)
    }

    private fun alertNoActivityRecognitionPermission() {
        Alerts.snackbar(
            context,
            getString(R.string.activity_recognition_permission_denied),
            short = false
        )
    }

    private fun alertNoCameraPermission() {
        Alerts.snackbar(
            context,
            getString(R.string.camera_permission_denied),
            short = false
        )
    }

    override fun openWidgets() {
        activity.openWidgets()
    }

    override fun reloadTheme() {
        activity.reloadTheme()
    }

    private fun requestPermissions(permissions: List<String>, action: () -> Unit) {
        val missingPermissions = permissions.filter {
            !Permissions.hasPermission(context, it)
        }
        if (missingPermissions.isEmpty()) {
            action()
            return
        }

        requestRuntimePermissions(missingPermissions.toTypedArray(), action)
    }

    private fun requestPermission(
        permission: SpecialPermission,
        rationale: PermissionRationale,
        action: () -> Unit
    ) {
        if (Permissions.hasPermission(context, permission)) {
            action()
            return
        }

        Alerts.dialog(
            context,
            rationale.title,
            rationale.message,
            okText = rationale.ok ?: getString(android.R.string.ok),
            cancelText = rationale.cancel ?: getString(android.R.string.cancel)
        ) { cancelled ->
            if (!cancelled) {
                specialPermissionAction = action
                Permissions.requestPermission(context, permission)
            } else {
                action()
            }
        }
    }

    /** While the screen is in the composition, quick actions and the activity reach it through this host. */
    fun attach() {
        activity.registerDestinationHost(this)
    }

    fun detach() {
        activity.unregisterDestinationHost(this)
    }

    /** Back from a system settings page (special permissions): run what was waiting on it. */
    fun onScreenResumed() {
        specialPermissionAction?.invoke()
        specialPermissionAction = null
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val destinationHost = this
        Box(modifier.fillMaxSize()) {
            DestinationContent(route, destinationHost, modifier)
            photoImportDialog?.Content()
            val currentActionSheet = actionSheet
            if (currentActionSheet != null) {
                TrailSenseSheet(
                    onDismissRequest = { dismissActionSheet(null) },
                    title = currentActionSheet.title
                ) {
                    ActionSheetContent(
                        subtitle = currentActionSheet.subtitle,
                        actions = currentActionSheet.actions,
                        stats = currentActionSheet.stats,
                        onAction = ::dismissActionSheet,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun dismissActionSheet(action: ActionItem?) {
        val current = actionSheet ?: return
        actionSheet = null
        current.onAction(action)
    }

    // ScreenEffects: the platform side of what screens ask for, on top of the host's callback APIs.

    override suspend fun requestPermission(permission: AppPermission): Boolean =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val resume: (Boolean) -> Unit = { if (continuation.isActive) continuation.resume(it) { } }
            when (permission) {
                AppPermission.Camera -> requestCamera(resume)
                AppPermission.ActivityRecognition -> requestActivityRecognition(resume)
                AppPermission.ExactAlarms -> requestScheduleExactAlarms(resume)
                AppPermission.Backtrack -> requestBacktrackPermission(resume)
                AppPermission.BackgroundLocation -> requestBackgroundLocation(resume)
                AppPermission.IgnoreBatteryOptimizations -> requestIgnoreBatteryOptimizations(resume)
            }
        }

    override fun explainPermissionDenied(permission: AppPermission) {
        when (permission) {
            AppPermission.Camera -> alertNoCameraPermission()
            AppPermission.ActivityRecognition -> alertNoActivityRecognitionPermission()
            else -> Unit
        }
    }

    override fun showMessage(message: String, long: Boolean) {
        message(message, short = !long)
    }

    override suspend fun showSnackbar(message: String, actionLabel: String?, long: Boolean): Boolean =
        AppHost.from(activity).showSnackbar(message, actionLabel, long)

    override suspend fun chooseAction(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        stats: List<TrailSenseStat>
    ): ActionItem? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        showActionSheet(title, subtitle, actions, stats) { if (continuation.isActive) continuation.resume(it) { } }
    }

    override suspend fun capturePhoto(maxWidth: Int?, maxHeight: Int?, zeroShutterLag: Boolean): String? =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val size = if (maxWidth != null && maxHeight != null) android.util.Size(maxWidth, maxHeight) else null
            showPhotoImport(size, zeroShutterLag) { if (continuation.isActive) continuation.resume(it?.toString()) { } }
        }

}

private data class ActionSheetState(
    val title: String,
    val subtitle: String?,
    val actions: List<ActionItem>,
    val stats: List<TrailSenseStat>,
    val onAction: (ActionItem?) -> Unit
)
