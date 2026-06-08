package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.os.Bundle
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R

object UserGuideUtils {

    fun showGuide(fragment: Fragment, @RawRes guideId: Int) {
        openGuide(fragment, guideId)
    }

    fun openGuide(fragment: Fragment, @RawRes guideId: Int) {
        val navController = fragment.findNavController()
        val guides = Guides.guides(fragment.requireContext())

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
