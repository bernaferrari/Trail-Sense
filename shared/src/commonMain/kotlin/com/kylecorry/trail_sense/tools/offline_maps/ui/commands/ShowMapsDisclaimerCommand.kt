package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import androidx.compose.ui.text.AnnotatedString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.shared.text.withWebLinks
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

/**
 * The offline maps safety disclaimer (carry a physical map, where to find map files), shown until the person closes
 * it once. Its second button opens the offline maps user guide.
 */
class ShowMapsDisclaimerCommand(
    private val host: AppHost,
    private val appNavigator: TrailSenseNavigator
) : Command {
    override fun execute() {
        val strings = getAppService<AppStrings>()
        Alerts.disclaimer(
            host,
            strings.get(Res.string.photo_maps),
            offlineMapsDisclaimerMessage(strings.get(Res.string.photo_map_disclaimer)),
            PreferenceKeys.offlineMapsDisclaimerShown,
            considerShownIfCancelled = true,
            cancelText = strings.get(Res.string.tool_user_guide_title),
            shownValue = true
        ) { showUserGuide, _ ->
            if (showUserGuide && appNavigator.currentRoute != null) {
                UserGuideUtils.showGuide(appNavigator, RawTexts.guide_tool_offline_maps)
            }
        }
    }
}

/** The disclaimer with its map download address clickable. */
internal fun offlineMapsDisclaimerMessage(text: String): AnnotatedString = text.withWebLinks()
