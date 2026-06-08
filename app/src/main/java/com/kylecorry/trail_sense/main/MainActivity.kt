package com.kylecorry.trail_sense.main

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
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
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils.setupWithNavController
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.PowerSavingModeAlertCommand
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivity : AndromedaActivity() {

    var isRunning = false
        private set

    private var mainViews: MainActivityViews? = null
    private val views: MainActivityViews
        get() = mainViews!!

    private val navController: NavController?
        get() = findNavController()

    val errorBanner: ErrorBannerState
        get() = views.errorBanner

    private val userPrefs = getAppService<UserPreferences>()
    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }

    private val themeProvider = ThemeProvider()

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

        mainViews = createMainActivityViews(this)
        setContentView(views.root)
        setupNavHost()

        // Handle black theme
        if (isBlackTheme) {
            window.decorView.rootView.setBackgroundColor(Color.BLACK)
            views.bottomNavigation.setBackgroundColor(Color.BLACK)
        }

        if (userPrefs.theme == UserPreferences.Theme.Night) {
            views.colorFilter.setColorFilter(
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

    private fun setupNavHost() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_holder) != null) {
            return
        }

        val navHost = NavHostFragment()
        supportFragmentManager.commitNow {
            replace(R.id.fragment_holder, navHost)
            setPrimaryNavigationFragment(navHost)
        }
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
            val isToolPinnedToNav =
                views.bottomNavigation.menu.findItem(selectedTool.navAction) != null
            if (isToolPinnedToNav) {
                selectedTool.navAction
            } else {
                R.id.action_experimental_tools
            }
        } else {
            R.id.action_experimental_tools
        }

        views.bottomNavigation.menu.findItem(idToSelect)?.isChecked = true

        lastKnownFragment = getFragment()?.javaClass?.simpleName
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
        views.bottomNavigation.apply {
            if (userPrefs.useCompactMode) {
                layoutParams.height = Resources.dp(context, 55f).toInt() + bottomInsets
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
            } else {
                layoutParams.height = LayoutParams.WRAP_CONTENT
                if (userPrefs.useShowAllBottomNavigationLabels) {
                    labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
                } else {
                    labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_AUTO
                }

            }
        }
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
            val item = views.bottomNavigation.menu.findItem(R.id.action_weather)
            item?.isVisible = false
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
            views.bottomNavigation.menu[0].itemId
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
        val fragment = getFragment() ?: return null
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
        views.bottomNavigation.menu.clear()
        bottomNavTools.forEachIndexed { index, toolId ->
            val toolItem = tools.firstOrNull { it.id == toolId } ?: return@forEachIndexed
            views.bottomNavigation.menu.add(
                Menu.NONE,
                toolItem.navAction,
                index,
                toolItem.name
            ).setIcon(toolItem.icon)
        }

        views.bottomNavigation.menu.add(
            Menu.NONE,
            R.id.action_experimental_tools,
            bottomNavTools.size,
            getString(R.string.tools)
        ).setIcon(R.drawable.apps)
            .setOnMenuItemClickListener {
                if (navController?.currentDestination?.id == R.id.action_experimental_tools && !views.quickActionsSheet.isOpen()) {
                    val searchinput = findViewById<TextInputEditText>(R.id.search_view_edit_text)
                    if (searchinput?.requestFocus() == true) {
                        val imm = getSystemService(InputMethodManager::class.java)
                        imm.showSoftInput(searchinput, InputMethodManager.SHOW_IMPLICIT)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }

        // Loop through each item of the bottom navigation and override the long press behavior
        for (i in 0 until views.bottomNavigation.menu.size) {
            val item = views.bottomNavigation.menu[i]
            val view = views.bottomNavigation.findViewById<View>(item.itemId)
            view.setOnLongClickListener {
                views.quickActionsSheet.show(this)
                true
            }
        }

        // Open the left most item by default (and clear the back stack)
        val initialItem = if (SafeMode.isEnabled()) {
            tools.first { it.id == Tools.SETTINGS }.navAction
        } else {
            views.bottomNavigation.menu[0].itemId
        }

        // Only initialize the nav graph once
        effect("navGraph") {
            initializeNavGraph(initialItem)
        }
        // Bind to navigation
        navController?.let { views.bottomNavigation.setupWithNavController(it, false) }

        updateBottomNavSelection()
    }

    private fun initializeNavGraph(startDestination: Int) {
        val nav = navController ?: return
        val navGraph = nav.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(startDestination)
        nav.graph = navGraph
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
        if (isEnabled) {
            views.bottomNavigation.enable()
        } else {
            views.bottomNavigation.disable()
        }
    }

    fun getBottomNavigationView(): View {
        return views.bottomNavigation
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

        var lastKnownFragment: String? = null

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
    val colorFilter: ColorFilterConstraintLayout,
    val errorBanner: ErrorBannerState,
    val fragmentHolder: FragmentContainerView,
    val quickActionsSheet: com.kylecorry.trail_sense.tools.tools.quickactions.QuickActionSheet,
    val bottomNavigation: CustomBottomNavigationView
)

private fun createMainActivityViews(context: Context): MainActivityViews {
    val root = CoordinatorLayout(context).apply {
        id = R.id.coordinator
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val colorFilter = ColorFilterConstraintLayout(context, null).apply {
        id = R.id.color_filter
    }
    root.addView(
        colorFilter,
        CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val errorBanner = ErrorBannerState(context)
    val errorBannerView = ComposeView(context).apply {
        id = R.id.error_banner
        setContent {
            TrailSenseTheme {
                errorBanner.Content()
            }
        }
    }
    colorFilter.addView(
        errorBannerView,
        LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = LayoutParams.PARENT_ID
            startToStart = LayoutParams.PARENT_ID
            endToEnd = LayoutParams.PARENT_ID
        }
    )

    val bottomNavigation = CustomBottomNavigationView(context, null).apply {
        id = R.id.bottom_navigation
        itemIconTintList = AppCompatResources.getColorStateList(context, R.drawable.selector)
        itemTextColor = ColorStateList.valueOf(
            Resources.getAndroidColorAttr(
                context,
                com.google.android.material.R.attr.colorOnSurface
            )
        )
    }
    colorFilter.addView(
        bottomNavigation,
        LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToBottom = LayoutParams.PARENT_ID
            startToStart = LayoutParams.PARENT_ID
            endToEnd = LayoutParams.PARENT_ID
        }
    )

    val fragmentHolder = FragmentContainerView(context).apply {
        id = R.id.fragment_holder
    }
    colorFilter.addView(
        fragmentHolder,
        LayoutParams(
            0,
            0
        ).apply {
            topToBottom = R.id.error_banner
            bottomToTop = R.id.bottom_navigation
            startToStart = LayoutParams.PARENT_ID
            endToEnd = LayoutParams.PARENT_ID
        }
    )

    val quickActionsSheet = com.kylecorry.trail_sense.tools.tools.quickactions.QuickActionSheet(
        context,
        null
    ).apply {
        id = R.id.quick_actions_sheet
    }
    colorFilter.addView(
        quickActionsSheet,
        LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToTop = R.id.bottom_navigation
            startToStart = LayoutParams.PARENT_ID
            endToEnd = LayoutParams.PARENT_ID
        }
    )

    return MainActivityViews(
        root,
        colorFilter,
        errorBanner,
        fragmentHolder,
        quickActionsSheet,
        bottomNavigation
    )
}
