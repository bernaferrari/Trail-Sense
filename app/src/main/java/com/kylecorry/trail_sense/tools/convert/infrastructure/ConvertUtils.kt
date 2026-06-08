package com.kylecorry.trail_sense.tools.convert.infrastructure

import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.convert.ui.ConverterTabs
import com.kylecorry.trail_sense.tools.convert.ui.CoordinateConverterPage
import com.kylecorry.trail_sense.tools.convert.ui.DistanceConverterPage
import com.kylecorry.trail_sense.tools.convert.ui.TemperatureConverterPage

object ConvertUtils {

    fun showConvert(fragment: Fragment): BottomSheetDialog {
        val context = fragment.requireContext()
        val sheet = BottomSheetDialog(context)
        val root = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            ConverterTabs(
                pages = listOf(
                    CoordinateConverterPage(context),
                    DistanceConverterPage(context),
                    TemperatureConverterPage(context)
                ),
                names = listOf(
                    context.getString(R.string.coordinates_tab),
                    context.getString(R.string.distance),
                    context.getString(R.string.temperature)
                )
            )
        }

        sheet.setContentView(root)
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                sheet.dismiss()
            }
        })
        sheet.show()
        return sheet
    }
}
