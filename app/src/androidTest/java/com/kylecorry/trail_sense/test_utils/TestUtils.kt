package com.kylecorry.trail_sense.test_utils

import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityWindowInfo
import java.util.regex.Pattern
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.views.TestView
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.shared.navigation.Route

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.work.testing.WorkManagerTestInitHelper
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.permissions.SpecialPermission
import com.kylecorry.andromeda.torch.TorchState
import com.kylecorry.andromeda.torch.torchStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.TrailSenseApplicationInitializer
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.findNavigator
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.packageName
import com.kylecorry.trail_sense.test_utils.views.childWithIndex
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.longClick
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule

object TestUtils {

    var inUseCameraIds: List<String> = emptyList()
        private set

    var isTorchOn: Boolean = false
        private set

    private var cameraAvailabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            inUseCameraIds = inUseCameraIds.filter { it != cameraId }
        }

        override fun onCameraUnavailable(cameraId: String) {
            inUseCameraIds = inUseCameraIds + cameraId
        }
    }

    private var torchJob: Job? = null

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun mute(): Int {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
        return currentVolume
    }

    fun unmute(volume: Int) {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun isPlayingMusic(): Boolean {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        return audioManager.isMusicActive
    }

    fun back(requireSuccess: Boolean = true) {
        // pressBack reports whether the key was injected and the UI idled in time. Compose transitions often keep
        // animating past that wait, so a back that worked can report false. Wait for the UI to settle instead and let
        // the test's next check verify where it landed.
        if (!device.pressBack() && requireSuccess) {
            device.waitForIdle()
        }
    }

    fun closeKeyboard() {
        val isKeyboardOpen = device.executeShellCommand("dumpsys input_method")
            .contains("mInputShown=true")
        if (isKeyboardOpen) {
            back(false)
        }
    }

    fun getString(@StringRes id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }

    fun setWaitForIdleTimeout(timeout: Long = 10) {
        Configurator.getInstance().setWaitForIdleTimeout(timeout)
    }

    fun listenForCameraUsage() {
        val manager = context.getSystemService<CameraManager>()
        manager?.registerAvailabilityCallback(
            cameraAvailabilityCallback,
            Handler(Looper.getMainLooper())
        )
    }

    fun stopListeningForCameraUsage() {
        val manager = context.getSystemService<CameraManager>()
        manager?.unregisterAvailabilityCallback(cameraAvailabilityCallback)
    }

    fun listenForTorchUsage() {
        torchJob?.cancel()
        torchJob = torchStates()
            .onEach { onTorchStateChanged(it == TorchState.On) }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    fun stopListeningForTorchUsage() {
        torchJob?.cancel()
        torchJob = null
    }

    fun not(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            return
        }
        throw Exception("Expected exception")
    }

    // STARTUP
    /**
     * Setup the application to match the actual application (Trail Sense application)
     */
    fun setupApplication() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        // Drop the DB
        AppDatabase.getInstance(context).clearAllTables()
        TrailSenseApplicationInitializer.initialize(context)
        setupDefaultPreferences()
    }

    fun setLocationOverride(coordinate: Coordinate) {
        val prefs = UserPreferences(context)
        prefs.gps.locationSource = GPSLocationSource.Manual
        prefs.gps.locationOverride = coordinate
    }

    fun runShellCommand(command: String) {
        device.executeShellCommand(command)
    }

    fun clearAppData(packageName: String) {
        runShellCommand("pm clear $packageName")
    }

    fun grantPermission(packageName: String, permission: String) {
        runShellCommand("pm grant $packageName $permission")
    }

    fun closeApp(packageName: String) {
        runShellCommand("am force-stop $packageName")
    }

    fun finishCurrentActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>()
                .firstOrNull()
                ?.finishAndRemoveTask()
        }
    }

    fun launchApp(packageName: String) {
        val packageManager = context.packageManager
        var intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        }
        if (intent == null) {
            intent = Intent(Intent.ACTION_MAIN).apply { setPackage(packageName) }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                intent.setComponent(
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name,
                    )
                )
            }
        }
        context.startActivity(intent)
    }

    fun startWithTool(
        toolId: Long,
        onActivityCallback: (activity: MainActivity) -> Unit = {}
    ): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            onActivityCallback(activity)
            if (toolId == 0L) {
                activity.findNavigator()!!.navigate(Route.Tools)
            } else {
                val tool = Tools.getTool(context, toolId)
                tool?.let {
                    activity.findNavigator()!!.navigate(tool.route)
                }
            }
        }
        return scenario
    }

    fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    fun openQuickActions() {
        // The keyboard covers the bottom navigation
        hideKeyboard()
        // Long pressing any bottom navigation tab opens the quick actions. The tabs have no tags, so the tab is the
        // long-clickable node lowest on the screen.
        waitFor {
            val pkg = packageName ?: context.packageName
            val tab = device.findObjects(By.longClickable(true).pkg(pkg))
                .maxByOrNull { it.visibleBounds.bottom }
                ?: throw AssertionError("Bottom navigation not found")
            tab.longClick()
        }
        waitFor {
            view(By.text(getString(R.string.quick_actions)))
        }
    }

    fun isKeyboardVisible(): Boolean {
        return InstrumentationRegistry.getInstrumentation().uiAutomation.windows.any {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }
    }

    fun hideKeyboard() {
        if (isKeyboardVisible()) {
            device.pressBack()
            waitFor { if (isKeyboardVisible()) throw AssertionError("The keyboard is still visible") }
        }
    }

    fun closeQuickActions() {
        click(desc(getString(android.R.string.cancel)))
    }

    // PERMISSIONS
    fun mainPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    fun allPermissionsGranted(): TestRule {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    // PREFERENCES
    fun setupDefaultPreferences() {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.clear()
        // Session state (such as a path being followed) outlives the database, so a test must not inherit it
        SessionStateSubsystem.getInstance(context).preferences.clear()

        val migrator = PreferenceMigrator.getInstance()
        migrator.migrate(context)

        prefs.putString(context.getString(R.string.pref_distance_units), "feet_miles")
        prefs.putString(context.getString(R.string.pref_weight_units), "lbs")
        prefs.putBoolean(context.getString(R.string.pref_use_24_hour), false)
        prefs.putBoolean(context.getString(R.string.pref_onboarding_completed), true)
        prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        prefs.putBoolean(context.getString(R.string.pref_cliff_height_enabled), true)
        prefs.putString(context.getString(R.string.pref_altimeter_calibration_mode), "gps")
        prefs.putInt(context.getString(R.string.pref_altimeter_accuracy), 1)
        val userPrefs = UserPreferences(context)
        userPrefs.gps.accuracyFilter = GPSAccuracyFilter.None
        // The settings tool is the fastest to get to idle, which allows the tests to run faster
        userPrefs.bottomNavigationTools = listOf(Tools.SETTINGS)
        userPrefs.useCompactMode = true
    }

    // WAITING
    fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun <T> waitFor(durationMillis: Long = 5000, action: () -> T): T {
        var remaining = durationMillis
        var times = 0
        var lastException: Throwable? = null
        val startTime = System.currentTimeMillis()
        while (remaining > 0 || times == 0) {
            try {
                return action()
            } catch (e: Throwable) {
                lastException = e
            }
            remaining = durationMillis - (System.currentTimeMillis() - startTime)
            times++
        }
        if (lastException != null) {
            throw lastException
        }
        throw Exception("Timeout")
    }

    // NOTIFICATIONS
    fun openNotificationShade() {
        device.openNotification()
    }

    fun closeNotificationShade() {
        // A swipe only collapses the shade one level when quick settings are expanded, so ask the system to dismiss it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            device.swipe(0, device.displayHeight, 0, 0, 10)
        }
        device.waitForIdle()
    }

    // Helpers
    fun matchesSelfOrChild(
        parent: UiObject2,
        depth: Int = 10,
        predicate: (obj: UiObject2) -> Boolean
    ): Boolean {
        if (depth == 0) {
            return false
        }

        if (predicate(parent)) {
            return true
        }

        for (child in parent.children) {
            if (matchesSelfOrChild(child, depth - 1, predicate)) {
                return true
            }
        }

        return false
    }

    fun getMatchingChild(
        parent: UiObject2,
        depth: Int = 10,
        predicate: (obj: UiObject2) -> Boolean
    ): UiObject2? {
        if (depth == 0) {
            return null
        }

        if (predicate(parent)) {
            return parent
        }

        for (child in parent.children) {
            val matchingChild = getMatchingChild(child, depth - 1, predicate)
            if (matchingChild != null) {
                return matchingChild
            }
        }

        return null
    }

    /**
     * Picks a date in the Material 3 date picker: switch it to text input, then type the date as digits (the field
     * adds the separators for the locale's pattern, month first in en-US).
     */
    fun pickDate(year: Int, month: Int, day: Int, waitForClose: Boolean = true) {
        waitFor {
            view(By.desc(Pattern.compile("(?i).*(text input|input mode).*"))).click()
        }

        waitFor {
            view(By.clazz("android.widget.EditText")).input(
                month.toString().padStart(2, '0') + day.toString().padStart(2, '0') + year.toString()
            )
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        if (waitForClose) {
            waitFor {
                not { view(By.clazz("android.widget.EditText")) }
            }
        }
    }

    /**
     * Picks a time on the Material 3 clock dial: tap the hour, then the minute (the dial labels every 5 minutes),
     * then AM or PM. The header repeats the current hour and minute, so the dial's copy is the lowest match.
     */
    fun pickTime(hour: Int, minute: Int, am: Boolean, waitForClose: Boolean = true) {
        require(minute % 5 == 0) { "The clock dial can only be tapped on multiples of 5 minutes" }

        // The View clock labels its numbers with text; the Compose clock only describes them ("8 o'clock")
        waitFor {
            lowestWithTextOrDesc(hour.toString(), Pattern.compile("${hour} o'clock")).click()
        }

        waitFor {
            lowestWithTextOrDesc(
                minute.toString().padStart(2, '0'),
                Pattern.compile("0?${minute} minutes?")
            ).click()
        }

        waitFor {
            view(By.text(if (am) "AM" else "PM")).click()
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        if (waitForClose) {
            waitFor {
                not { viewWithText(android.R.string.ok) }
            }
        }
    }

    private fun lowestWithTextOrDesc(text: String, desc: Pattern): TestView {
        val node = (device.findObjects(By.text(text)) + device.findObjects(By.desc(desc)))
            .maxByOrNull { it.visibleBounds.top }
            ?: throw AssertionError("Text not found: $text")
        return TestView(node)
    }


    fun isCameraInUse(isBackFacing: Boolean? = null): Boolean {
        val manager = context.getSystemService<CameraManager>() ?: return false
        for (cameraId in inUseCameraIds) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT && (isBackFacing == null || !isBackFacing)) {
                return true
            }
            if (facing == CameraCharacteristics.LENS_FACING_BACK && (isBackFacing == null || isBackFacing)) {
                return true
            }
        }
        return false
    }

    /**
     * Opens a list row's overflow menu and picks [label]. Compose rows expose the overflow button by its
     * content description ("More options" unless the screen names it otherwise) or by a testTag.
     */
    fun clickListItemMenu(
        label: String,
        index: Int = 0,
        menu: () -> TestView = { view(By.desc(getString(R.string.note_more_options)), index) }
    ) {
        click(menu)
        click(label)
    }

    fun handleExactAlarmsDialog() {
        // TODO: Add the option to grant the permission
        if (!Permissions.hasPermission(
                context,
                SpecialPermission.SCHEDULE_EXACT_ALARMS
            )
        ) {
            click("Cancel")
        }
    }

    private fun onTorchStateChanged(isOn: Boolean) {
        isTorchOn = isOn
    }
}
