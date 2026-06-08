package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen

object UserGuideUtils {

    fun showGuide(fragment: Fragment, @RawRes guideId: Int): BottomSheetDialog? {
        val guides = Guides.guides(fragment.requireContext())
        val guide =
            guides.flatMap { it.guides }.firstOrNull { it.contents == guideId } ?: return null
        val sheet = BottomSheetDialog(fragment.requireContext())
        val root = TrailSenseComposeRootView(
            fragment.requireContext(),
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            GuideScreen(name = guide.name, resource = guide.contents)
        }
        sheet.setContentView(root)
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                sheet.dismiss()
            }
        })
        sheet.show()
        return sheet
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
