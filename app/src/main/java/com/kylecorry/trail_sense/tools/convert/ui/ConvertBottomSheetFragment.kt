package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class ConvertBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    ConverterTabs(
                        pages = getPages(),
                        names = getPageNames()
                    )
                }
            }
        }
    }

    private fun getPages(): List<ConverterPage> {
        val context = requireContext()
        return listOf(
            FragmentToolCoordinateConvert(context),
            FragmentDistanceConverter(context),
            FragmentTemperatureConverter(context)
        )
    }

    private fun getPageNames(): List<String> {
        return listOf(
            getString(R.string.coordinates_tab),
            getString(R.string.distance),
            getString(R.string.temperature)
        )
    }
}
