package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseBottomSheetComposeFragment
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

class GuideBottomSheetFragment : TrailSenseBottomSheetComposeFragment() {

    private val guide: UserGuide by lazy {
        UserGuide(
            name = requireArguments().getString(ARG_GUIDE_NAME) ?: "",
            contents = requireArguments().getInt(ARG_GUIDE_CONTENTS)
        )
    }

    @Composable
    override fun FragmentContent() {
        GuideScreen(name = guide.name, resource = guide.contents)
    }

    companion object {
        private const val ARG_GUIDE_NAME = "guide_name"
        private const val ARG_GUIDE_CONTENTS = "guide_contents"

        fun create(guide: UserGuide): GuideBottomSheetFragment {
            return GuideBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GUIDE_NAME, guide.name)
                    putInt(ARG_GUIDE_CONTENTS, guide.contents)
                }
            }
        }
    }
}
