package com.kylecorry.trail_sense.main.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.theme.ThemeRepository
import com.kylecorry.trail_sense.shared.views.TrailSenseColorScheme
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/**
 * Top-level composable for the Navigation 3 path.
 *
 * Renders a [NavigationBar] at the bottom (mirroring the legacy
 * `ActivityMainBinding.bottomNavigation` behaviour) and a [NavDisplay]
 * that switches between route content.
 *
 * @param initialRoute the route to display when the composable is first composed.
 * @param pendingRoute a route pushed by the Activity (e.g. from intent handling)
 *   that should be navigated to once.
 */
@Composable
fun TrailSenseApp(
    initialRoute: AppRoute = AppRoute.Tools,
    pendingRoute: AppRoute? = null
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    val themeRepo = remember { ThemeRepository.getInstance(context) }
    val initialScheme = if (prefs.useDynamicColors) TrailSenseColorScheme.Dynamic
    else TrailSenseColorScheme.Orange
    val colorScheme by themeRepo.colorScheme.collectAsState(initial = initialScheme)

    val isDarkTheme = when (prefs.theme) {
        UserPreferences.Theme.Light -> false
        UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> true
        UserPreferences.Theme.System, UserPreferences.Theme.SystemBlack -> androidx.compose.foundation.isSystemInDarkTheme()
        UserPreferences.Theme.SunriseSunset -> false
    }
    val isAmoled = prefs.theme == UserPreferences.Theme.Black ||
            prefs.theme == UserPreferences.Theme.Night ||
            (isDarkTheme && prefs.theme == UserPreferences.Theme.SystemBlack)

    TrailSenseTheme(
        colorScheme = colorScheme,
        darkTheme = isDarkTheme,
        isAmoled = isAmoled
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            val bottomNavTools = remember { prefs.bottomNavigationTools }
            val allTools = remember { Tools.getTools(context) }
            val navBarItems = remember(bottomNavTools, allTools) {
                buildNavBarItems(bottomNavTools, allTools, context)
            }

            val backStack = rememberNavBackStack(initialRoute)

            // Push pending route from Activity intent handling
            LaunchedEffect(pendingRoute) {
                pendingRoute?.let {
                    if (backStack.lastOrNull() != it) {
                        backStack.add(it)
                    }
                }
            }

            val currentRoute = backStack.lastOrNull()
            val selectedNavBarId = remember(currentRoute) {
                deriveSelectedNavBarId(currentRoute, navBarItems)
            }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        navBarItems.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                selected = item.toolId == selectedNavBarId,
                                onClick = {
                                    val target = item.route
                                    if (backStack.lastOrNull() != target) {
                                        // Pop back to root before navigating to a top-level destination
                                        while (backStack.size > 1) {
                                            backStack.removeLastOrNull()
                                        }
                                        if (backStack.lastOrNull() != target) {
                                            backStack.add(target)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TrailSenseNav3Host(
                        backStack = backStack,
                        onNavigateToTool = { navActionId ->
                            val route = AppRoute.fromNavId(navActionId) ?: AppRoute.Tools
                            backStack.add(route)
                        }
                    )
                }
            }
        }
    }
}

private data class NavBarItem(
    val toolId: Long,
    val icon: Int,
    val label: String,
    val route: AppRoute
)

private fun buildNavBarItems(
    bottomNavToolIds: List<Long>,
    allTools: List<Tool>,
    context: Context
): List<NavBarItem> {
    val items = mutableListOf<NavBarItem>()

    bottomNavToolIds.filter { it != 0L }.forEach { toolId ->
        val tool = allTools.firstOrNull { it.id == toolId } ?: return@forEach
        val route = AppRoute.fromNavId(tool.navAction) ?: return@forEach
        items.add(
            NavBarItem(
                toolId = tool.id,
                icon = tool.icon,
                label = tool.name,
                route = route
            )
        )
    }

    // Always append the Tools tab
    items.add(
        NavBarItem(
            toolId = Tools.TOOLS_TAB_ID,
            icon = R.drawable.apps,
            label = context.getString(R.string.tools),
            route = AppRoute.Tools
        )
    )

    return items
}

private fun deriveSelectedNavBarId(currentRoute: NavKey?, items: List<NavBarItem>): Long {
    val route = currentRoute as? AppRoute ?: return Tools.TOOLS_TAB_ID
    return items.firstOrNull { it.route == route }?.toolId ?: Tools.TOOLS_TAB_ID
}

private val Tools.TOOLS_TAB_ID: Long get() = -1L
