package com.kylecorry.trail_sense.tools.tools.ui

import android.app.PendingIntent
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
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
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool

/**
 * The Tools tab: every tool, pinned ones first; reselecting the tab focuses the search. [onController] offers the
 * activity the screen's [ToolSearchFocusable] while it is shown (null once it leaves).
 */
@Composable
fun ToolsHomeScreen(onController: (ToolSearchFocusable?) -> Unit) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val controller = remember(context, navigator) { ToolsHomeController(context, navigator) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { onController(null) }
    }
    ToolsScreen(
        pinnedToolIds = controller.pinnedToolManager.pinnedIds.value,
        onToolClick = { tool ->
            navigator.navigate(tool.route)
        },
        onToolLongClick = controller::showToolActions,
        onSettingsClick = {
            navigator.navigate(Route.Settings)
        },
        onSearchFocusRequestChanged = {
            controller.focusSearchRequest = it
        }
    )
}

private class ToolsHomeController(
    private val context: Context,
    private val navigator: TrailSenseNavigator
) : ToolSearchFocusable {
    val pinnedToolManager by lazy { PinnedToolManager(UserPreferences(context)) }
    var focusSearchRequest: (() -> Boolean)? = null

    override fun focusSearch(): Boolean {
        return focusSearchRequest?.invoke() == true
    }

    fun showToolActions(tool: Tool) {
        val actions = listOfNotNull(
            if (tool.isExperimental) context.getString(R.string.experimental) else null,
            if (tool.description != null) context.getString(R.string.pref_category_about) else null,
            if (pinnedToolManager.isPinned(tool.id)) context.getString(R.string.unpin) else context.getString(R.string.pin),
            if (tool.guideId != null) context.getString(R.string.tool_user_guide_title) else null,
            if (tool.settingsRoute != null) context.getString(R.string.settings) else null,
            context.getString(R.string.add_to_homescreen)
        )

        Pickers.item(context, tool.name, actions) { selected ->
            when (actions.getOrNull(selected ?: return@item)) {
                context.getString(R.string.experimental) -> Unit
                context.getString(R.string.pref_category_about) -> Alerts.dialog(
                    context,
                    tool.name,
                    tool.description,
                    cancelText = null
                )
                context.getString(R.string.unpin) -> pinnedToolManager.unpin(tool.id)
                context.getString(R.string.pin) -> pinnedToolManager.pin(tool.id)
                context.getString(R.string.tool_user_guide_title) -> UserGuideUtils.showGuide(
                    context,
                    navigator,
                    tool.guideId!!
                )
                context.getString(R.string.settings) -> navigator.navigate(tool.settingsRoute!!)
                context.getString(R.string.add_to_homescreen) -> addToHomescreen(tool)
            }
        }
    }

    private fun addToHomescreen(tool: Tool) {
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
