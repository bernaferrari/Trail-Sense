package com.kylecorry.trail_sense.tools.ballistics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class FragmentToolBallistics : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    BallisticsTabs()
                }
            }
        }
    }
}

@Composable
private fun BallisticsTabs(
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
