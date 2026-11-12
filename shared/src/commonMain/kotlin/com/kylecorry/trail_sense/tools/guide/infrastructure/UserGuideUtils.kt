package com.kylecorry.trail_sense.tools.guide.infrastructure

import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.RawText

object UserGuideUtils {

    fun showGuide(appNavigator: TrailSenseNavigator, guideId: RawText) {
        openGuide(appNavigator, guideId)
    }

    fun openGuide(appNavigator: TrailSenseNavigator, guideId: RawText) {
        val guides = Guides.guides()

        val guide = guides.flatMap { it.guides }.firstOrNull { it.contents == guideId }

        if (guide != null) {
            appNavigator.navigate(Route.Guide(name = guide.name, contents = guide.contents))
        }
    }

}
