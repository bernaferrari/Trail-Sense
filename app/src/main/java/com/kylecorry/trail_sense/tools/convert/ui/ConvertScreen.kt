@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R

@Composable
fun ConvertScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages = remember(context) {
        listOf(
            CoordinateConverterPage(context),
            DistanceConverterPage(context),
            TemperatureConverterPage(context),
            VolumeConverterPage(context),
            WeightConverterPage(context),
            TimeConverterPage(context)
        )
    }
    val names = listOf(
        stringResource(R.string.coordinates_tab),
        stringResource(R.string.distance),
        stringResource(R.string.temperature),
        stringResource(R.string.volume),
        stringResource(R.string.weight),
        stringResource(R.string.time)
    )
    ConverterTabs(
        pages = pages,
        names = names,
        modifier = modifier
    )
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
