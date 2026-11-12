package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.shared.text.withWebLinks
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowMapsDisclaimerCommand(
    private val context: Context,
    private val appNavigator: TrailSenseNavigator
) : Command {
    override fun execute() {
        val message = context.getString(R.string.photo_map_disclaimer).withWebLinks()
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
                    appNavigator,
                    RawTexts.guide_tool_offline_maps
                )
            }
        }
    }
}
