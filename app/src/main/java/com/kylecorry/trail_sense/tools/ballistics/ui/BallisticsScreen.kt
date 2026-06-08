@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.ballistics.ui

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
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R

@Composable
fun BallisticsScreen(
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val names = listOf(
        stringResource(R.string.scope),
        stringResource(R.string.ballistics),
        stringResource(R.string.energy)
    )

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

        when (selectedIndex) {
            0 -> ScopeAdjustmentPage()
            1 -> BallisticsCalculatorPage()
            2 -> EnergyCalculatorPage()
        }
    }
}
