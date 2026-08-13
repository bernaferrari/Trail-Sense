@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

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

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = TrailSenseSize.maximumContentWidth)
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.screen)
        ) {
            names.forEachIndexed { index, name ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = { Text(name, style = MaterialTheme.typography.labelLarge) }
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
    }
}
