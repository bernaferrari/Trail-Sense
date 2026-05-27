package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

/**
 * Bottom sheet variant of guide detail. Reuses the modern GuideContent composable.
 * Shown programmatically via UserGuideUtils (show() extension handles presentation).
 */
class GuideBottomSheetFragment : TrailSenseComposeFragment() {

    private val guide: UserGuide by lazy {
        UserGuide(
            name = requireArguments().getString(ARG_GUIDE_NAME) ?: "",
            contents = requireArguments().getInt(ARG_GUIDE_CONTENTS)
        )
    }

    @Composable
    override fun FragmentContent() {
        // Bridge the lazy guide into the shared composable via args (consistent with GuideFragment)
        // We set arguments in create(), so the GuideContent extension reads them.
        GuideContent()
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
