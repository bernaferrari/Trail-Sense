package com.kylecorry.trail_sense.main

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.errors.ExceptionHandler
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.errors.TrailSenseExceptionHandler
import com.kylecorry.trail_sense.main.theme.ThemeProvider
import com.kylecorry.trail_sense.onboarding.OnboardingActivity
import com.kylecorry.trail_sense.receivers.RestartServicesCommand
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.ComposedCommand
import com.kylecorry.trail_sense.shared.navigation.TrailSenseComposeNavHost
import com.kylecorry.trail_sense.shared.navigation.TrailSenseComposeNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavGraph
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.PowerSavingModeAlertCommand
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.quickactions.QuickActionSheetState
import com.kylecorry.trail_sense.tools.tools.ui.ToolSearchFocusable

class MainActivity : AndromedaActivity() {

    var isRunning = false
        private set

    private var mainViews: MainActivityViews? = null
    private val views: MainActivityViews
        get() = mainViews!!

    private val navController: NavController?
        get() = rootNavController

    val errorBanner: ErrorBannerState
        get() = views.errorBanner

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

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    override fun attachBaseContext(newBase: Context) {
        // This is needed to prevent old colors from remaining when the theme changes
        // Dynamic colors are only disabled until onCreate is called since it needs the activity to be created
        setColorTheme(themeProvider.getColorTheme(), false)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)

        updateTheme()

        val isBlackTheme =
            userPrefs.theme == UserPreferences.Theme.Black
                    || userPrefs.theme == UserPreferences.Theme.Night
                    || (isDarkThemeOn() && userPrefs.theme == UserPreferences.Theme.SystemBlack)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (cache.getBoolean(getString(R.string.pref_onboarding_completed)) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            return
        }

        Screen.setAllowScreenshots(window, !userPrefs.privacy.isScreenshotProtectionOn)

        setupNavController()
        mainViews = createMainActivityViews(
            activity = this,
            navigator = composeNavigator ?: error("Compose navigator is not initialized")
        )
        setContentView(views.root)

        // Handle black theme
        if (isBlackTheme) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            views.bottomNavigation.useBlackTheme = true
        }

        if (userPrefs.theme == UserPreferences.Theme.Night) {
            setColorFilter(
                views.colorFilter,
                PorterDuffColorFilter(
                    Color.RED,
                    PorterDuff.Mode.MULTIPLY
                )
            )
        }

        updateFullscreenMode()
        bindLayoutInsets()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViews = null
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
            val isToolPinnedToNav = views.bottomNavigation.contains(selectedTool.navAction)
            if (isToolPinnedToNav) {
                selectedTool.navAction
            } else {
                R.id.action_experimental_tools
            }
        } else {
            R.id.action_experimental_tools
        }

        views.bottomNavigation.selectedItemId = idToSelect

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
        views.bottomNavigation.useCompactMode = userPrefs.useCompactMode
        views.bottomNavigation.showAllLabels = userPrefs.useShowAllBottomNavigationLabels
        views.bottomNavigation.bottomInsetDp =
            (bottomInsets / resources.displayMetrics.density).toInt()
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(views.root) { v, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                rightMargin = insets.right
            }
            bottomInsets = insets.bottom
            setBottomNavLabelsVisibility()
            windowInsets
        }
    }

    private fun updateTheme() {
        val mode = themeProvider.getColorTheme()
        setColorTheme(mode, userPrefs.useDynamicColors)
    }

    fun reloadTheme() {
        updateTheme()
        updateFullscreenMode()
        recreate()
    }

    override fun onStart() {
        super.onStart()
        if (!appInitialized && mainViews != null) {
            appInitialized = true
            updateBottomNavigation()

            navController?.addOnDestinationChangedListener { _, _, _ ->
                views.quickActionsSheet.close()
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
                views.bottomNavigation.selectedItemId,
                null,
                NavOptions.Builder().setPopUpTo(
                    navController?.currentDestination?.id ?: R.id.action_experimental_tools, true
                ).build()
            )
        }

        ComposedCommand(
            ShowDisclaimerCommand(this),
            PowerSavingModeAlertCommand(this),
            RestartServicesCommand(this, false),
            NotifyForkPoliciesCommand(this)
        ).execute()

        if (!Tools.isToolAvailable(this, Tools.WEATHER)) {
            views.bottomNavigation.items.replaceAll {
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
                views.bottomNavigation.selectedItemId = tool.navAction
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
        views.bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            views.bottomNavigation.items.firstOrNull()?.id ?: R.id.action_experimental_tools
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
            outState.putInt("page", views.bottomNavigation.selectedItemId)
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
        val fragment = activeDestinationHost ?: return null
        val tools = Tools.getTools(this)

        // Sort order = High priority active, open tool, normal priority active

        val highPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.High && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0),
                    fragment
                )
            }
        }.firstOrNull()

        if (highPriorityAction != null) {
            return highPriorityAction.create(fragment)
        }

        val activeAction =
            tools.firstOrNull { it.isOpen(navigationId ?: 0) }?.volumeActions?.firstOrNull {
                it.isActive(this, true, fragment)
            }

        if (activeAction != null) {
            return activeAction.create(fragment)
        }

        val normalPriorityAction = tools.flatMap { tool ->
            tool.volumeActions.filter {
                it.priority == ToolVolumeActionPriority.Normal && it.isActive(
                    this,
                    tool.isOpen(navigationId ?: 0),
                    fragment
                )
            }
        }.firstOrNull()

        return normalPriorityAction?.create?.invoke(fragment)
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
        views.bottomNavigation.items.clear()
        bottomNavTools.forEachIndexed { index, toolId ->
            val toolItem = tools.firstOrNull { it.id == toolId } ?: return@forEachIndexed
            views.bottomNavigation.items.add(
                MainBottomNavigationItem(
                    id = toolItem.navAction,
                    title = toolItem.name,
                    icon = toolItem.icon
                )
            )
        }

        views.bottomNavigation.items.add(
            MainBottomNavigationItem(
                id = R.id.action_experimental_tools,
                title = getString(R.string.tools),
                icon = R.drawable.apps
            )
        )

        // Open the left most item by default (and clear the back stack)
        val initialItem = if (SafeMode.isEnabled()) {
            tools.first { it.id == Tools.SETTINGS }.navAction
        } else {
            views.bottomNavigation.items.firstOrNull()?.id ?: R.id.action_experimental_tools
        }

        // Only initialize the nav graph once
        effect("navGraph") {
            initializeNavGraph(initialItem)
        }
        updateBottomNavSelection()
    }

    fun onBottomNavigationItemSelected(itemId: Int) {
        if (itemId == R.id.action_experimental_tools &&
            navController?.currentDestination?.id == R.id.action_experimental_tools &&
            !views.quickActionsSheet.isOpen()
        ) {
            val toolsFragment = activeDestinationHost as? ToolSearchFocusable
            if (toolsFragment?.focusSearch() == true) {
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
        views.bottomNavigation.selectedItemId = itemId
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
        views.quickActionsSheet.show(this, 1)
    }

    fun setBottomNavigationEnabled(isEnabled: Boolean) {
        views.bottomNavigation.isEnabled = isEnabled
    }

    fun getBottomNavigationHeight(): Int {
        val navigationHeight = if (userPrefs.useCompactMode) 55f else 80f
        return Resources.dp(this, navigationHeight).toInt() + bottomInsets
    }

    fun addOverlayView(view: View) {
        views.root.addView(view)
    }

    fun removeOverlayView(view: View) {
        views.root.removeView(view)
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

private data class MainActivityViews(
    val root: CoordinatorLayout,
    val colorFilter: View,
    val errorBanner: ErrorBannerState,
    val navigator: TrailSenseComposeNavigator,
    val quickActionsSheet: QuickActionSheetState,
    val bottomNavigation: MainBottomNavigationState
)

private fun createMainActivityViews(
    activity: MainActivity,
    navigator: TrailSenseComposeNavigator
): MainActivityViews {
    val context = activity
    val root = CoordinatorLayout(context).apply {
        id = R.id.coordinator
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val errorBanner = ErrorBannerState(context)
    val bottomNavigation = MainBottomNavigationState()
    val quickActionsSheet = QuickActionSheetState(context)

    val colorFilter = TrailSenseComposeRootView(
        context,
        ViewCompositionStrategy.DisposeOnDetachedFromWindow
    ) {
        MainActivityContent(
            errorBanner = errorBanner,
            navigator = navigator,
            quickActionsSheet = quickActionsSheet,
            bottomNavigation = bottomNavigation,
            onBottomNavigationItemSelected = activity::onBottomNavigationItemSelected,
            onBottomNavigationItemLongSelected = {
                quickActionsSheet.show(activity)
            }
        )
    }.apply {
        id = R.id.color_filter
    }
    root.addView(
        colorFilter,
        CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    return MainActivityViews(
        root,
        colorFilter,
        errorBanner,
        navigator,
        quickActionsSheet,
        bottomNavigation
    )
}

@Suppress("FunctionName")
@Composable
private fun MainActivityContent(
    errorBanner: ErrorBannerState,
    navigator: TrailSenseComposeNavigator,
    quickActionsSheet: QuickActionSheetState,
    bottomNavigation: MainBottomNavigationState,
    onBottomNavigationItemSelected: (Int) -> Unit,
    onBottomNavigationItemLongSelected: (Int) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            errorBanner.Content()
            TrailSenseComposeNavHost(
                navigator = navigator,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
            MainBottomNavigation(
                state = bottomNavigation,
                onItemClick = onBottomNavigationItemSelected,
                onItemLongClick = onBottomNavigationItemLongSelected
            )
        }

        val navigationHeight = if (bottomNavigation.useCompactMode) 55 else 80
        quickActionsSheet.Content(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (navigationHeight + bottomNavigation.bottomInsetDp).dp)
        )
    }
}

private fun setColorFilter(view: View, filter: ColorFilter?) {
    val paint = Paint().apply {
        colorFilter = filter
    }
    if (filter != null) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    } else {
        view.setLayerType(View.LAYER_TYPE_NONE, paint)
    }
}
