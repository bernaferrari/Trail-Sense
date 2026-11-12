package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class ShowTideDisclaimerCommand(
    private val host: AppHost,
    private val appNavigator: TrailSenseNavigator,
    private val onAgree: () -> Unit
) : Command {
    override fun execute() {
        val strings = getAppService<AppStrings>()
        val message = tideDisclaimerMessage(
            strings.get(Res.string.disclaimer_estimated_tide),
            strings.get(Res.string.tide_instructions),
            strings.get(Res.string.tide_user_guide_recommendation)
        )
        Alerts.disclaimer(
            host,
            strings.get(Res.string.tides),
            message,
            PreferenceKeys.tideDisclaimerSeen,
            considerShownIfCancelled = true,
            cancelText = strings.get(Res.string.tool_user_guide_title)
        ) { showGuide, _ ->
            if (showGuide) {
                UserGuideUtils.openGuide(appNavigator, RawTexts.guide_tool_tides)
            } else {
                onAgree()
            }
        }
    }
}

/** The disclaimer, then the instructions in bold, then where to find tide tables. */
internal fun tideDisclaimerMessage(
    disclaimer: String,
    instructions: String,
    tableLocation: String
): AnnotatedString {
    return buildAnnotatedString {
        append(disclaimer)
        append("\n\n")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(instructions)
        }
        append("\n\n")
        append(tableLocation)
    }
}
