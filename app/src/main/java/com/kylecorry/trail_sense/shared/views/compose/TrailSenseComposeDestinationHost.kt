package com.kylecorry.trail_sense.shared.views.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
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
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.markdown.MarkdownService
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
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.ActionSheetContent
import com.kylecorry.trail_sense.tools.map.ui.MapControllerHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class TrailSenseComposeDestinationHost(
    private val context: Context,
    private val activity: MainActivity,
    private val rootView: View,
    override val route: Route,
    override val lifecycleOwner: LifecycleOwner,
    private val requestRuntimePermissions: (Array<String>, () -> Unit) -> Unit
) : TrailSenseReactiveDestinationHost {

    private var actionSheet by mutableStateOf<ActionSheetState?>(null)
    private var photoImportDialog by mutableStateOf<PhotoImportDialog?>(null)
    override var screenController: Any? = null
    internal var mapController: MapControllerHost? = null
    private var resumeCount = 0
    private var mapUpdatesEnabled = true
    private var reactiveUpdateScheduled = false
    private var specialPermissionAction: (() -> Unit)? = null
    internal val reactiveComponent = TrailSenseReactiveComponent(
        contextProvider = ::requireContext,
        activityProvider = ::requireActivity,
        rootViewProvider = ::requireView,
        lifecycleOwnerProvider = { lifecycleOwner },
        viewLifecycleOwnerProvider = { lifecycleOwner },
        onStateChanged = ::onReactiveUpdate
    )

    fun requireActivity(): MainActivity {
        return activity
    }

    fun requireView(): View {
        return rootView
    }

    override val lifecycleScope: CoroutineScope
        get() = lifecycleOwner.lifecycleScope

    override val mainActivity: MainActivity
        get() = activity

    override val intentResultRetriever: IntentResultRetriever
        get() = activity


    override fun requireContext(): Context {
        return context
    }

    override fun getResources(): android.content.res.Resources {
        return context.resources
    }

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any?): String {
        return context.getString(resId, *formatArgs)
    }

    override fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    override val hookResetOnResume: Int
        get() = resumeCount

    override fun cleanupReactiveEffects() {
        reactiveComponent.cleanupEffects()
    }

    override fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        onAction: (ActionItem?) -> Unit
    ) {
        actionSheet = ActionSheetState(title, subtitle, actions, onAction)
    }

    override fun showSnackbar(
        text: String,
        duration: Int,
        action: String?,
        onAction: () -> Unit
    ): Snackbar {
        val playBar = rootView.findViewWithTag<View>(getString(R.string.tag_play_bar))
        return Snackbar.make(playBar ?: requireView(), text, duration).apply {
            if (action != null) {
                setAction(action) { onAction() }
            }
            show()
        }
    }

    override fun showPhotoImport(
        resolution: android.util.Size?,
        useZeroShutterLag: Boolean,
        onCapture: (android.net.Uri?) -> Unit
    ) {
        photoImportDialog = PhotoImportDialog(
            requireContext(),
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

    override fun requestCamera(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.CAMERA)) {
            action(Camera.isAvailable(requireContext()))
        }
    }

    override fun requestActivityRecognition(action: (hasPermission: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(listOf(Manifest.permission.ACTIVITY_RECOGNITION)) {
                action(Permissions.canRecognizeActivity(requireContext()))
            }
        } else {
            action(true)
        }
    }

    override fun requestScheduleExactAlarms(action: (hasPermission: Boolean) -> Unit) {
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
                requireContext(),
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
            if (!isGranted) {
                Alerts.toast(
                    requireContext(),
                    getString(R.string.exact_alarm_permission_denied),
                    short = false
                )
            }
            action(isGranted)
        }
    }

    override fun requestBacktrackPermission(action: (hasPermission: Boolean) -> Unit) {
        if (Permissions.canStartLocationForgroundService(requireContext())) {
            action(true)
            return
        }

        requestPermissions(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            val hasPermission = Permissions.canStartLocationForgroundService(requireContext())
            if (!hasPermission) {
                Alerts.toast(requireContext(), getString(R.string.backtrack_no_permission))
            }
            action(hasPermission)
        }
    }

    override fun requestBackgroundLocation(action: (hasPermission: Boolean) -> Unit) {
        requestPermissions(listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            action(Permissions.isBackgroundLocationEnabled(requireContext()))
        }
    }

    override fun requestIgnoreBatteryOptimizations(action: (hasPermission: Boolean) -> Unit) {
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
                requireContext(),
                SpecialPermission.IGNORE_BATTERY_OPTIMIZATIONS
            )
            if (!isGranted) {
                toast(getString(R.string.battery_usage_restricted), short = false)
            }
            action(isGranted)
        }
    }

    override fun findNavigator(): TrailSenseNavigator {
        return requireMainActivity().navigator
            ?: error("Root TrailSenseNavigator is not available")
    }

    override fun toast(message: CharSequence, short: Boolean) {
        Alerts.toast(requireContext(), message, short)
    }

    override fun inBackground(
        state: BackgroundMinimumState,
        cancelWhenRerun: Boolean,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return lifecycleOwner.inBackground(state, true, cancelWhenRerun, block)
    }

    override fun alertNoActivityRecognitionPermission() {
        Alerts.toast(
            requireContext(),
            getString(R.string.activity_recognition_permission_denied),
            short = false
        )
    }

    override fun alertNoCameraPermission() {
        Alerts.toast(
            requireContext(),
            getString(R.string.camera_permission_denied),
            short = false
        )
    }

    override fun openWidgets() {
        requireMainActivity().openWidgets()
    }

    override fun reloadTheme() {
        requireMainActivity().reloadTheme()
    }

    private fun requestPermissions(permissions: List<String>, action: () -> Unit) {
        val missingPermissions = permissions.filter {
            !Permissions.hasPermission(requireContext(), it)
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
        if (Permissions.hasPermission(requireContext(), permission)) {
            action()
            return
        }

        Alerts.dialog(
            requireContext(),
            rationale.title,
            rationale.message,
            okText = rationale.ok ?: getString(android.R.string.ok),
            cancelText = rationale.cancel ?: getString(android.R.string.cancel)
        ) { cancelled ->
            if (!cancelled) {
                specialPermissionAction = action
                Permissions.requestPermission(requireContext(), permission)
            } else {
                action()
            }
        }
    }

    fun onStart() {
        requireMainActivity().registerDestinationHost(this)
    }

    fun onStop() {
        requireMainActivity().unregisterDestinationHost(this)
    }

    fun onResume() {
        resumeCount++
        mapUpdatesEnabled = true
        specialPermissionAction?.invoke()
        specialPermissionAction = null
        if (route == Route.Map) {
            onReactiveUpdate()
        }
    }

    fun onPause() {
        mapUpdatesEnabled = false
        if (route == Route.Map) {
            mapController?.onPause()
        }
    }

    internal fun onReactiveUpdate() {
        if (
            !mapUpdatesEnabled ||
            reactiveUpdateScheduled ||
            route != Route.Map
        ) {
            return
        }

        reactiveUpdateScheduled = true
        rootView.post {
            reactiveUpdateScheduled = false
            if (!mapUpdatesEnabled) {
                return@post
            }
            reactiveComponent.resetHookCursor()
            mapController?.onUpdate()
        }
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

    override fun getBottomNavigationHeight(): Int {
        return requireMainActivity().getBottomNavigationHeight()
    }

    override fun addOverlayView(view: View) {
        requireMainActivity().addOverlayView(view)
    }

    override fun removeOverlayView(view: View) {
        requireMainActivity().removeOverlayView(view)
    }

}

private data class ActionSheetState(
    val title: String,
    val subtitle: String?,
    val actions: List<ActionItem>,
    val onAction: (ActionItem?) -> Unit
)
