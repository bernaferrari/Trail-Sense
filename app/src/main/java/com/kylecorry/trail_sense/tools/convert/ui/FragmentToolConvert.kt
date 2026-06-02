package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class FragmentToolConvert : Fragment() {

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
            FragmentTemperatureConverter(context),
            FragmentVolumeConverter(context),
            FragmentWeightConverter(context),
            FragmentTimeConverter(context)
        )
    }

    private fun getPageNames(): List<String> {
        return listOf(
            getString(R.string.coordinates_tab),
            getString(R.string.distance),
            getString(R.string.temperature),
            getString(R.string.volume),
            getString(R.string.weight),
            getString(R.string.time)
        )
    }
}

@Composable
fun ConverterTabs(
    pages: List<ConverterPage>,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex) {
            names.forEachIndexed { index, name ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = { Text(name) }
                )
            }
        }
        pages[selectedIndex].Content()
    }
}
