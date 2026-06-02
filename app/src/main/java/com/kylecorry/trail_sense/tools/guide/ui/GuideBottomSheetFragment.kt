package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

class GuideBottomSheetFragment : BottomSheetDialogFragment() {

    private val guide: UserGuide by lazy {
        UserGuide(
            name = requireArguments().getString(ARG_GUIDE_NAME) ?: "",
            contents = requireArguments().getInt(ARG_GUIDE_CONTENTS)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    GuideScreen(name = guide.name, resource = guide.contents)
                }
            }
        }
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
