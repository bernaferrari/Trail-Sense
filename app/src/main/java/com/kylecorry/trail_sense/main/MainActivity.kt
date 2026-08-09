package com.kylecorry.trail_sense.main

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.android.material.color.DynamicColors
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.errors.ExceptionHandler
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.errors.TrailSenseExceptionHandler
import com.kylecorry.trail_sense.main.theme.AppThemeMode
import com.kylecorry.trail_sense.main.theme.ThemeProvider
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.RestartServicesCommand
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.commands.ComposedCommand
import com.kylecorry.trail_sense.shared.navigation.TrailSenseComposeNavHost
import com.kylecorry.trail_sense.shared.navigation.TrailSenseComposeNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavGraph
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.main.theme.TrailSenseAppTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.quickactions.QuickActionSheetState
import com.kylecorry.trail_sense.tools.tools.ui.ToolSearchFocusable

class MainActivity : ComponentActivity(), IntentResultRetriever {

    var isRunning = false
        private set

    private val navController: NavController?
        get() = rootNavController

    val errorBanner by lazy { ErrorBannerState(this) }
    private val quickActionsSheet by lazy { QuickActionSheetState(this) }
    private val bottomNavigation = MainBottomNavigationState()

    private val userPrefs = getAppService<UserPreferences>()
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val themeProvider = ThemeProvider()
    private var rootNavController: NavController? = null
    private var composeNavigator: TrailSenseComposeNavigator? = null
    private var activeDestinationHost: TrailSenseDestinationHost? = null

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var bottomInsets = 0
    private var appInitialized = false
    private var navGraphInitialized = false
    private var permissionResultAction: (() -> Unit)? = null
    private var intentResultAction: ((Boolean, Intent?) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionResultAction?.invoke()
            permissionResultAction = null
        }

    private val intentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            intentResultAction?.invoke(
                result.resultCode == Activity.RESULT_OK,
                result.data
            )
            intentResultAction = null
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    override fun attachBaseContext(newBase: Context) {
        // This is needed to prevent old colors from remaining when the theme changes
        // Dynamic colors are only disabled until onCreate is called since it needs the activity to be created
        applyThemeMode(themeProvider.getThemeMode(), false)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)

        updateTheme()

        val isBlackTheme =
            userPrefs.theme == UserPreferences.Theme.Black
                    || userPrefs.theme == UserPreferences.Theme.Night
                    || (isDarkThemeOn() && userPrefs.theme == UserPreferences.Theme.SystemBlack)
        val useNightColorFilter = userPrefs.theme == UserPreferences.Theme.Night

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        Screen.setAllowScreenshots(window, !userPrefs.privacy.isScreenshotProtectionOn)

        setupNavController()

        setContent {
            val darkTheme = when (themeProvider.getThemeMode()) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System -> isSystemInDarkTheme()
            }
            TrailSenseAppTheme(
                darkTheme = darkTheme,
                isAmoled = isBlackTheme,
                useDynamicColors = userPrefs.useDynamicColors
            ) {
                MainActivityContent(
                    errorBanner = errorBanner,
                    navigator = composeNavigator ?: error("Compose navigator is not initialized"),
                    quickActionsSheet = quickActionsSheet,
                    bottomNavigation = bottomNavigation,
                    nightColorFilter = useNightColorFilter,
                    onBottomNavigationItemSelected = ::onBottomNavigationItemSelected,
                    onBottomNavigationItemLongSelected = { quickActionsSheet.show(this) }
                )
            }
        }

        // Handle black theme
        if (isBlackTheme) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            bottomNavigation.useBlackTheme = true
        }

        updateFullscreenMode()
        bindLayoutInsets()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setupNavController() {
        val navController = NavController(this)
        val navigator = TrailSenseComposeNavigator()
        navController.setLifecycleOwner(this)
        navController.setOnBackPressedDispatcher(onBackPressedDispatcher)
        navController.setViewModelStore(viewModelStore)
        navController.navigatorProvider.addNavigator(navigator)
        rootNavController = navController
        composeNavigator = navigator
    }

    fun getRootNavController(): NavController? {
        return rootNavController
    }

    /**
     * Update the bottom nav selection based on the current navigation destination
     */
    private fun updateBottomNavSelection() {
        val currentNavId = navController?.currentDestination?.id
        val tools = Tools.getTools(this)
        val selectedTool = tools.firstOrNull { it.isOpen(currentNavId ?: 0) }
        // If the tool appears in the bottom nav, select it. Otherwise, select the Tools tab
        val idToSelect = if (selectedTool != null) {
            val isToolPinnedToNav = bottomNavigation.contains(selectedTool.navAction)
            if (isToolPinnedToNav) {
                selectedTool.navAction
            } else {
                R.id.action_experimental_tools
            }
        } else {
            R.id.action_experimental_tools
        }

        bottomNavigation.selectedItemId = idToSelect

        lastKnownDestinationHost = activeDestinationHost?.javaClass?.simpleName
    }

    fun registerDestinationHost(host: TrailSenseDestinationHost) {
        activeDestinationHost = host
        lastKnownDestinationHost = host.javaClass.simpleName
    }

    fun unregisterDestinationHost(host: TrailSenseDestinationHost) {
        if (activeDestinationHost == host) {
            activeDestinationHost = null
        }
    }

    fun getActiveDestinationHost(): TrailSenseDestinationHost? {
        return activeDestinationHost
    }

    fun changeBottomNavLabelsVisibility(useCompactMode: Boolean) {
        userPrefs.useCompactMode = useCompactMode
        setBottomNavLabelsVisibility()
    }

    fun showAllBottomNavLabels(useBottomNavLabels: Boolean) {
        userPrefs.useShowAllBottomNavigationLabels = useBottomNavLabels
        setBottomNavLabelsVisibility()
    }

    private fun setBottomNavLabelsVisibility() {
        bottomNavigation.useCompactMode = userPrefs.useCompactMode
        bottomNavigation.showAllLabels = userPrefs.useShowAllBottomNavigationLabels
        bottomNavigation.bottomInsetDp =
            (bottomInsets / resources.displayMetrics.density).toInt()
    }

    private fun bindLayoutInsets() {
        // Harvest bottom inset (and system bars/cutout info) for bottom nav height calculations
        // and legacy getBottomNavigationHeight() / overlay positioning. We no longer mutate
        // a view root's margins; Compose MainActivityContent applies windowInsetsPadding for
        // top + horizontal safe area.
        val rootView = findViewById<View>(android.R.id.content) ?: window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            bottomInsets = insets.bottom
            setBottomNavLabelsVisibility()
            windowInsets
        }
    }

    private fun updateTheme() {
        val mode = themeProvider.getThemeMode()
        applyThemeMode(mode, userPrefs.useDynamicColors)
    }

    private fun applyThemeMode(mode: AppThemeMode, useDynamicColors: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                AppThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        if (useDynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }

    private fun requestPermissions(permissions: List<String>, action: () -> Unit) {
        val missingPermissions = permissions.filter {
            !Permissions.hasPermission(this, it)
        }
        if (missingPermissions.isEmpty()) {
            action()
            return
        }

        permissionResultAction = action
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    override fun getResult(intent: Intent, action: (Boolean, Intent?) -> Unit) {
        intentResultAction = action
        intentResultLauncher.launch(intent)
    }

    fun reloadTheme() {
        updateTheme()
        updateFullscreenMode()
        recreate()
    }

    override fun onStart() {
        super.onStart()
        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            return
        }
        if (!appInitialized) {
            appInitialized = true
            updateBottomNavigation()

            navController?.addOnDestinationChangedListener { _, _, _ ->
                quickActionsSheet.close()
                updateBottomNavSelection()
            }

            val previousPermissionStatus = permissions.map {
                Permissions.hasPermission(this, it)
            }
            requestPermissions(permissions) {
                val currentPermissionStatus = permissions.map {
                    Permissions.hasPermission(this, it)
                }
                val permissionsChanged =
                    previousPermissionStatus.zip(currentPermissionStatus).any { it.first != it.second }
                startApp(permissionsChanged)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        updateAllWidgets()
        updateDeviceSurfaceRotation()
        FlashlightSubsystem.getInstance(this).startSystemMonitor()
        PedometerSubsystem.getInstance(this).recalculateState()
        Tools.subscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            ::onPowerSavingModeChanged
        )
        Tools.subscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            ::onPowerSavingModeChanged
        )
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        FlashlightSubsystem.getInstance(this).stopSystemMonitor()
        Tools.unsubscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED,
            ::onPowerSavingModeChanged
        )
        Tools.unsubscribe(
            BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED,
            ::onPowerSavingModeChanged
        )
    }

    private fun startApp(shouldReloadNavigation: Boolean) {
        errorBanner.dismissAll()

        if (shouldReloadNavigation) {
            navController?.navigate(
                bottomNavigation.selectedItemId,
                null,
                NavOptions.Builder().setPopUpTo(
                    navController?.currentDestination?.id ?: R.id.action_experimental_tools, true
                ).build()
            )
        }

        ComposedCommand(
            ShowDisclaimerCommand(this),
            RestartServicesCommand(this, false),
            NotifyForkPoliciesCommand(this)
        ).execute()

        if (!Tools.isToolAvailable(this, Tools.WEATHER)) {
            bottomNavigation.items.replaceAll {
                if (it.id == R.id.action_weather) {
                    it.copy(visible = false)
                } else {
                    it
                }
            }
        }

        handleIntentAction(intent)

        if (SafeMode.isEnabled()) {
            Alerts.toast(
                this,
                getString(R.string.safe_mode_toast),
                false
            )
        }
    }

    private fun handleIntentAction(intent: Intent) {
        if (intent.action == TrailSenseExceptionHandler.ACTION_TOOL_ERROR) {
            val toolId = intent.getLongExtra(TrailSenseExceptionHandler.EXTRA_TOOL_ID, 0L)
            val error = intent.getStringExtra(TrailSenseExceptionHandler.EXTRA_ERROR) ?: ""
            navController?.navigate(
                R.id.fragmentToolErrorHandler,
                Bundle().apply {
                    putLong("tool_id", toolId)
                    putString("error", error)
                }
            )
            return
        }

        if (intent.action == "com.kylecorry.trail_sense.OPEN_TOOL") {
            val toolId = intent.getLongExtra("tool_id", -1)
            val tool = Tools.getTool(this, toolId)
            if (tool != null) {
                if (navController?.currentDestination?.id != tool.navAction) {
                    navController?.navigate(tool.navAction)
                }
                bottomNavigation.selectedItemId = tool.navAction
            }
            return
        }

        if (intent.action == "com.kylecorry.trail_sense.NAVIGATE") {
            val destination = intent.getIntExtra("destination", 0)
            val args = intent.getBundleExtra("arguments")
            if (destination != 0) {
                navController?.navigate(destination, args)
            }
            return
        }

        val tools = Tools.getTools(this)
        tools.forEach { tool ->
            tool.intentHandlers.forEach { handler ->
                if (handler.handle(this, intent)) {
                    return
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentAction(intent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            bottomNavigation.items.firstOrNull()?.id ?: R.id.action_experimental_tools
        )
        if (savedInstanceState.containsKey("navigation")) {
            tryOrNothing {
                val bundle = savedInstanceState.getBundle("navigation_arguments")
                navController?.navigate(savedInstanceState.getInt("navigation"), bundle)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tryOrLog {
            outState.putInt("page", bottomNavigation.selectedItemId)
            navController?.currentBackStackEntry?.arguments?.let {
                outState.putBundle("navigation_arguments", it)
            }
            navController?.currentDestination?.id?.let {
                outState.putInt("navigation", it)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = true)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = true)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return onVolumePressed(isVolumeUp = false, isButtonPressed = false)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return onVolumePressed(isVolumeUp = true, isButtonPressed = false)
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun getVolumeAction(): VolumeAction? {
        val navigationId = navController?.currentDestination?.id
        val host = activeDestinationHost ?: return null
        val tools = Tools.getTools(this)

        // Sort order = High priority active, open tool, normal priority active

        val highPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.High && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0),
                    host
                )
            }
        }.firstOrNull()

        if (highPriorityAction != null) {
            return highPriorityAction.create(host)
        }

        val activeAction =
            tools.firstOrNull { it.isOpen(navigationId ?: 0) }?.volumeActions?.firstOrNull {
                it.isActive(this, true, host)
            }

        if (activeAction != null) {
            return activeAction.create(host)
        }

        val normalPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.Normal && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0),
                    host
                )
            }
        }.firstOrNull()

        return normalPriorityAction?.create?.invoke(host)
    }

    private fun onVolumePressed(isVolumeUp: Boolean, isButtonPressed: Boolean): Boolean {
        val action = getVolumeAction()

        if (action != null) {
            return if (isButtonPressed) {
                action.onButtonPress(isVolumeUp)
            } else {
                action.onButtonRelease(isVolumeUp)
            }
        }

        return false
    }

    fun updateBottomNavigation() {
        setBottomNavLabelsVisibility()

        val bottomNavTools = userPrefs.bottomNavigationTools

        val tools = Tools.getTools(this)
        bottomNavigation.items.clear()
        bottomNavTools.forEachIndexed { index, toolId ->
            val toolItem = tools.firstOrNull { it.id == toolId } ?: return@forEachIndexed
            bottomNavigation.items.add(
                MainBottomNavigationItem(
                    id = toolItem.navAction,
                    title = toolItem.name,
                    icon = toolItem.icon,
                    selectedIcon = getPrimaryNavigationIcons(toolId)?.first,
                    unselectedIcon = getPrimaryNavigationIcons(toolId)?.second
                )
            )
        }

        bottomNavigation.items.add(
            MainBottomNavigationItem(
                id = R.id.action_experimental_tools,
                title = getString(R.string.tools),
                icon = R.drawable.apps,
                selectedIcon = TrailSenseIcons.Filled.Apps,
                unselectedIcon = TrailSenseIcons.Outlined.Apps
            )
        )

        // Open the left most item by default (and clear the back stack)
        val initialItem = if (SafeMode.isEnabled()) {
            tools.first { it.id == Tools.SETTINGS }.navAction
        } else {
            bottomNavigation.items.firstOrNull()?.id ?: R.id.action_experimental_tools
        }

        if (!navGraphInitialized) {
            navGraphInitialized = true
            initializeNavGraph(initialItem)
        }
        updateBottomNavSelection()
    }

    private fun getPrimaryNavigationIcons(toolId: Long): Pair<ImageVector, ImageVector>? {
        return when (toolId) {
            Tools.NAVIGATION -> TrailSenseIcons.Filled.Explore to TrailSenseIcons.Outlined.Explore
            Tools.MAP -> TrailSenseIcons.Filled.Map to TrailSenseIcons.Outlined.Map
            Tools.WEATHER -> TrailSenseIcons.Filled.PartlyCloudyDay to
                    TrailSenseIcons.Outlined.PartlyCloudyDay
            Tools.ASTRONOMY -> TrailSenseIcons.Filled.Stars2 to TrailSenseIcons.Outlined.Stars2
            else -> null
        }
    }

    fun onBottomNavigationItemSelected(itemId: Int) {
        if (itemId == R.id.action_experimental_tools &&
            navController?.currentDestination?.id == R.id.action_experimental_tools &&
            !quickActionsSheet.isOpen()
        ) {
            val toolsHost = activeDestinationHost as? ToolSearchFocusable
            if (toolsHost?.focusSearch() == true) {
                return
            }
        }

        val nav = navController ?: return
        if (nav.currentDestination?.id != itemId) {
            nav.navigate(
                itemId,
                null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(nav.graph.startDestinationId, false)
                    .build()
            )
        }
        bottomNavigation.selectedItemId = itemId
    }

    private fun initializeNavGraph(startDestination: Int) {
        val nav = navController ?: return
        nav.graph = TrailSenseNavGraph.create(nav, startDestination)
    }

    private fun onPowerSavingModeChanged(data: Bundle): Boolean {
        recreate()
        return true
    }

    private fun updateAllWidgets() {
        Tools.getTools(this).flatMap { it.widgets }.forEach {
            Tools.triggerWidgetUpdate(this, it.id)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateDeviceSurfaceRotation() {
        deviceSurfaceRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: windowManager.defaultDisplay.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
    }

    fun openWidgets() {
        quickActionsSheet.show(this, 1)
    }

    fun setBottomNavigationEnabled(isEnabled: Boolean) {
        bottomNavigation.isEnabled = isEnabled
    }

    fun getBottomNavigationHeight(): Int {
        val navigationHeight = if (userPrefs.useCompactMode) 55f else 80f
        return Resources.dp(this, navigationHeight).toInt() + bottomInsets
    }

    fun addOverlayView(view: View) {
        val root = findViewById<ViewGroup>(android.R.id.content)
        root?.addView(view)
    }

    fun removeOverlayView(view: View) {
        val root = findViewById<ViewGroup>(android.R.id.content)
        root?.removeView(view)
    }

    private fun updateFullscreenMode() {
        val isNightMode = userPrefs.theme == UserPreferences.Theme.Night
        val shouldBeFullscreen = isNightMode && userPrefs.nightModeFullscreen

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        if (shouldBeFullscreen) {
            // Hide system bars (status bar and navigation bar)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            // Set behavior to show bars temporarily when user swipes from edge
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Show system bars
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {

        var deviceSurfaceRotation: Int = Surface.ROTATION_90
            private set

        var lastKnownDestinationHost: String? = null

        fun intent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }

        fun openToolIntent(context: Context, toolId: Long): Intent {
            val tool = Tools.getTool(context, toolId)!!
            return Intent(context, MainActivity::class.java).apply {
                this.action = "com.kylecorry.trail_sense.OPEN_TOOL"
                this.putExtra("tool_id", tool.id)
            }
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                27383254,
                intent(context),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

}

@Suppress("FunctionName")
@Composable
private fun MainActivityContent(
    errorBanner: ErrorBannerState,
    navigator: TrailSenseComposeNavigator,
    quickActionsSheet: QuickActionSheetState,
    bottomNavigation: MainBottomNavigationState,
    nightColorFilter: Boolean,
    onBottomNavigationItemSelected: (Int) -> Unit,
    onBottomNavigationItemLongSelected: (Int) -> Unit
) {
    val rootInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.End)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (nightColorFilter) {
                    colorFilter = ColorFilter.tint(
                        ComposeColor.Red,
                        BlendMode.Multiply
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(rootInsets)
        ) {
            errorBanner.Content()
            MainAdaptiveNavigation(
                state = bottomNavigation,
                onItemClick = onBottomNavigationItemSelected,
                onItemLongClick = onBottomNavigationItemLongSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TrailSenseComposeNavHost(
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize()
                    )
                    quickActionsSheet.Content(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
