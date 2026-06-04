package com.kylecorry.trail_sense.tools.tools.ui

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool

class ToolsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { com.kylecorry.trail_sense.shared.UserPreferences(requireContext()) }
    private val pinnedToolManager by lazy { PinnedToolManager(prefs) }

    @Composable
    override fun FragmentContent() {
        ToolsScreen(
            onToolClick = { tool ->
                findNavController().navigateWithAnimation(tool.navAction)
            },
            onToolLongClick = { tool ->
                showToolActions(tool)
            },
            onSettingsClick = {
                findNavController().navigate(R.id.action_settings)
            }
        )
    }

    private fun showToolActions(tool: Tool) {
        val actions = listOfNotNull(
            if (tool.isExperimental) getString(R.string.experimental) else null,
            if (tool.description != null) getString(R.string.pref_category_about) else null,
            if (pinnedToolManager.isPinned(tool.id)) getString(R.string.unpin) else getString(R.string.pin),
            if (tool.guideId != null) getString(R.string.tool_user_guide_title) else null,
            if (tool.settingsNavAction != null) getString(R.string.settings) else null,
            getString(R.string.add_to_homescreen)
        )

        Pickers.item(requireContext(), tool.name, actions) { selected ->
            when (actions.getOrNull(selected ?: return@item)) {
                getString(R.string.experimental) -> Unit
                getString(R.string.pref_category_about) -> dialog(tool.name, tool.description, cancelText = null)
                getString(R.string.unpin) -> pinnedToolManager.unpin(tool.id)
                getString(R.string.pin) -> pinnedToolManager.pin(tool.id)
                getString(R.string.tool_user_guide_title) -> UserGuideUtils.showGuide(this, tool.guideId!!)
                getString(R.string.settings) -> findNavController().navigateWithAnimation(tool.settingsNavAction!!)
                getString(R.string.add_to_homescreen) -> addToHomescreen(tool)
            }
        }
    }

    private fun addToHomescreen(tool: Tool) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
            return
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(requireContext(), "tool-${tool.id}")
            .setIcon(
                IconCompat.createFromIcon(
                    requireContext(),
                    Icon.createWithResource(requireContext(), tool.icon)
                )
            )
            .setIntent(MainActivity.openToolIntent(requireContext(), tool.id))
            .setShortLabel(tool.name)
            .setLongLabel(tool.name)
            .build()

        val callbackIntent =
            ShortcutManagerCompat.createShortcutResultIntent(requireContext(), shortcutInfo)
        val successCallback = PendingIntent.getBroadcast(
            requireContext(),
            0,
            callbackIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        ShortcutManagerCompat.requestPinShortcut(
            requireContext(),
            shortcutInfo,
            successCallback.intentSender
        )
    }
}
