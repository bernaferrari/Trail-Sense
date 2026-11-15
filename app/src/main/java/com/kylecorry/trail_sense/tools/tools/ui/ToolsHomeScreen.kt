package com.kylecorry.trail_sense.tools.tools.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.shared.dialogs.oneTimeSnackbar
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.item

/**
 * The Tools tab: every tool, pinned ones first; reselecting the tab focuses the search. [onController] offers the
 * activity the screen's [ToolSearchFocusable] while it is shown (null once it leaves).
 */
@Composable
fun ToolsHomeScreen(
    quickActionHost: AndroidDestinationHost,
    onController: (ToolSearchFocusable?) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val pinnedTools = remember { PinnedToolManager(UserPreferences(appContext)) }
    // The user's chosen quick actions (Settings > Tools), shown above the tool grid as they were before Compose
    val lifecycleOwner = LocalLifecycleOwner.current
    val quickActionIds = remember { UserPreferences(appContext).toolQuickActions.sorted() }
    val quickActionButtons = remember(quickActionIds) { quickActionIds.map { ComposeQuickActionButtonView() } }
    DisposableEffect(quickActionHost, lifecycleOwner, quickActionButtons) {
        val factory = QuickActionFactory()
        val actions = quickActionIds.zip(quickActionButtons) { id, button ->
            factory.create(id, button, quickActionHost).also { it.bind(lifecycleOwner) }
        }
        onDispose { actions.forEach { it.unbind(lifecycleOwner) } }
    }
    val tools = remember { Tools.getTools(context) }
    ToolsHomeContent(
        tools = tools,
        pinnedTools = pinnedTools,
        keywordReader = getAppService(),
        onShowLongPressHint = {
            Alerts.oneTimeSnackbar(
                AppHost.from(context), context.getString(R.string.tool_long_press_hint_toast),
                "tools_long_press_notice_shown", short = false
            )
        },
        quickActions = quickActionButtons,
        onToolClick = { tool ->
            navigator.navigate(tool.route)
        },
        onToolLongClick = { tool, model -> showToolActions(context, navigator, model, tool) },
        onSettingsClick = {
            navigator.navigate(Route.Settings)
        },
        onController = onController
    )
}

private fun showToolActions(
    context: Context,
    navigator: TrailSenseNavigator,
    viewModel: ToolsHomeViewModel,
    tool: Tool
) {
    val actions = listOfNotNull(
        if (tool.isExperimental) context.getString(R.string.experimental) else null,
        if (tool.description != null) context.getString(R.string.pref_category_about) else null,
        if (viewModel.isPinned(tool.id)) context.getString(R.string.unpin) else context.getString(R.string.pin),
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
            context.getString(R.string.unpin) -> viewModel.unpin(tool.id)
            context.getString(R.string.pin) -> viewModel.pin(tool.id)
            context.getString(R.string.tool_user_guide_title) -> UserGuideUtils.showGuide(
                navigator,
                tool.guideId!!
            )
            context.getString(R.string.settings) -> navigator.navigate(tool.settingsRoute!!)
            context.getString(R.string.add_to_homescreen) -> addToHomescreen(context, tool)
        }
    }
}

private fun addToHomescreen(context: Context, tool: Tool) {
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
