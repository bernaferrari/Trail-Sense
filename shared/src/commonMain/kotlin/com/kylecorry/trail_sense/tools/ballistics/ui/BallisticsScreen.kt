@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack

/** Ballistics: three calculators behind one segmented control, each a plain form under it. */
@Composable
fun BallisticsScreen(
    modifier: Modifier = Modifier
) {
    // One question: what do I dial, or what does this round do?
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val options = listOf(
        TrailSenseChoice(0, appString(Res.string.scope)),
        TrailSenseChoice(1, appString(Res.string.ballistics)),
        TrailSenseChoice(2, appString(Res.string.energy))
    )

    TrailSensePage(
        title = appString(Res.string.ballistics),
        onBack = rememberTrailSenseBack(),
        backLabel = appString(Res.string.back),
        modifier = modifier
    ) {
        item("calculator") {
            TrailSenseConnectedChoice(
                options = options,
                selected = selectedIndex,
                onSelect = { selectedIndex = it }
            )
        }
        item("page") {
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) },
                label = "ballisticsPage"
            ) { index ->
                when (index) {
                    0 -> ScopeAdjustmentPage()
                    1 -> BallisticsCalculatorPage()
                    else -> EnergyCalculatorPage()
                }
            }
        }
    }
}
