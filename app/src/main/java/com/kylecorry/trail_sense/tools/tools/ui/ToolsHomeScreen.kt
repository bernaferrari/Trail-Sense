package com.kylecorry.trail_sense.tools.tools.ui

import android.app.PendingIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.icons.toIconCompat
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.ScreenControllerEffect
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool

/** The Tools tab: every tool, pinned ones first; reselecting the tab focuses the search. */
@Composable
fun ToolsHomeScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { ToolsHomeController(host) }
    ScreenControllerEffect(host, controller)
    ToolsScreen(
        pinnedToolIds = controller.pinnedToolManager.pinnedIds.value,
        onToolClick = { tool ->
            host.findNavigator().navigate(tool.route)
        },
        onToolLongClick = controller::showToolActions,
        onSettingsClick = {
            host.findNavigator().navigate(Route.Settings)
        },
        onSearchFocusRequestChanged = {
            controller.focusSearchRequest = it
        }
    )
}

private class ToolsHomeController(private val host: TrailSenseDestinationHost) : ToolSearchFocusable {
    val pinnedToolManager by lazy { PinnedToolManager(UserPreferences(host.requireContext())) }
    var focusSearchRequest: (() -> Boolean)? = null

    override fun focusSearch(): Boolean {
        return focusSearchRequest?.invoke() == true
    }

    fun showToolActions(tool: Tool) {
        val context = host.requireContext()
        val actions = listOfNotNull(
            if (tool.isExperimental) host.getString(R.string.experimental) else null,
            if (tool.description != null) host.getString(R.string.pref_category_about) else null,
            if (pinnedToolManager.isPinned(tool.id)) host.getString(R.string.unpin) else host.getString(R.string.pin),
            if (tool.guideId != null) host.getString(R.string.tool_user_guide_title) else null,
            if (tool.settingsRoute != null) host.getString(R.string.settings) else null,
            host.getString(R.string.add_to_homescreen)
        )

        Pickers.item(context, tool.name, actions) { selected ->
            when (actions.getOrNull(selected ?: return@item)) {
                host.getString(R.string.experimental) -> Unit
                host.getString(R.string.pref_category_about) -> Alerts.dialog(
                    context,
                    tool.name,
                    tool.description,
                    cancelText = null
                )
                host.getString(R.string.unpin) -> pinnedToolManager.unpin(tool.id)
                host.getString(R.string.pin) -> pinnedToolManager.pin(tool.id)
                host.getString(R.string.tool_user_guide_title) -> UserGuideUtils.showGuide(
                    context,
                    host.findNavigator(),
                    tool.guideId!!
                )
                host.getString(R.string.settings) -> host.findNavigator().navigate(tool.settingsRoute!!)
                host.getString(R.string.add_to_homescreen) -> addToHomescreen(tool)
            }
        }
    }

    private fun addToHomescreen(tool: Tool) {
        val context = host.requireContext()
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "tool-${tool.id}")
            .setIcon(tool.icon.toIconCompat(context, 48f))
            .setIntent(MainActivity.openToolIntent(context, tool.id))
            .setShortLabel(tool.name)
            .setLongLabel(tool.name)
            .build()

        val callbackIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcutInfo)
        val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            callbackIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        ShortcutManagerCompat.requestPinShortcut(
            context,
            shortcutInfo,
            successCallback.intentSender
        )
    }
}
