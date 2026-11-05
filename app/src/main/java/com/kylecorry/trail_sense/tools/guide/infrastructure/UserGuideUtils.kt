package com.kylecorry.trail_sense.tools.guide.infrastructure

import com.kylecorry.trail_sense.shared.navigation.Route

import android.content.Context
import android.os.Bundle
import androidx.annotation.RawRes
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.R

object UserGuideUtils {

    fun showGuide(context: Context, appNavigator: TrailSenseNavigator, @RawRes guideId: Int) {
        openGuide(context, appNavigator, guideId)
    }

    fun openGuide(context: Context, appNavigator: TrailSenseNavigator, @RawRes guideId: Int) {
        val guides = Guides.guides(context)

        val guide = guides.flatMap { it.guides }.firstOrNull { it.contents == guideId }

        if (guide != null) {
            appNavigator.navigate(Route.Guide(name = guide.name, contents = guide.contents))
        }
    }

}
