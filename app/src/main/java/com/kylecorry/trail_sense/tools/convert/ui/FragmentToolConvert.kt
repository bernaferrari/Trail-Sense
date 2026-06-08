package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment

class FragmentToolConvert : TrailSenseComposeFragment() {

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
            TemperatureConverterPage(context),
            VolumeConverterPage(context),
            WeightConverterPage(context),
            TimeConverterPage(context)
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
