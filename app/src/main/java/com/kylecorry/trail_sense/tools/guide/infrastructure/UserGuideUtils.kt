package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.navigation.NavController
import com.kylecorry.trail_sense.R

object UserGuideUtils {

    fun showGuide(context: Context, navController: NavController, @RawRes guideId: Int) {
        openGuide(context, navController, guideId)
    }

    fun openGuide(context: Context, navController: NavController, @RawRes guideId: Int) {
        val guides = Guides.guides(context)

        val guide = guides.flatMap { it.guides }.firstOrNull { it.contents == guideId }

        if (guide != null) {
            navController.navigate(
                R.id.guideFragment, Bundle().apply {
                    putString("guide_name", guide.name)
                    putInt("guide_contents", guide.contents)
                }
            )
        }
    }

}
