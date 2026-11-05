package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import android.text.util.Linkify
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowMapsDisclaimerCommand(
    private val context: Context,
    private val appNavigator: TrailSenseNavigator
) : Command {
    override fun execute() {
        val message = context.getString(R.string.photo_map_disclaimer).toSpannable()
        LinkifyCompat.addLinks(message, Linkify.WEB_URLS)
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.photo_maps),
            message,
            context.getString(R.string.photo_map_disclaimer_shown),
            okText = context.getString(android.R.string.ok),
            considerShownIfCancelled = true,
            cancelText = context.getString(R.string.tool_user_guide_title),
            shownValue = true
        ) { showUserGuide, _ ->
            if (showUserGuide && appNavigator.currentRoute != null) {
                UserGuideUtils.showGuide(
                    context,
                    appNavigator,
                    R.raw.guide_tool_offline_maps
                )
            }
        }
    }
}
