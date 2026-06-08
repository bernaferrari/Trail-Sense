package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseBottomSheetComposeFragment

class ConvertBottomSheetFragment : TrailSenseBottomSheetComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ConverterTabs(
            pages = getPages(),
            names = getPageNames()
        )
    }

    private fun getPages(): List<ConverterPage> {
        val context = requireContext()
        return listOf(
            CoordinateConverterPage(context),
            DistanceConverterPage(context),
            TemperatureConverterPage(context)
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
