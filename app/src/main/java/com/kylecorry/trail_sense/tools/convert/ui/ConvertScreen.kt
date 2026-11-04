@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack

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

/**
 * Convert as a standard page: what you're converting is the kit's selection control, and the chosen converter fades
 * in beneath it.
 */
@Composable
fun ConverterTabs(
    pages: List<ConverterPage>,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    // One question: what is this amount in the other unit?
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    TrailSensePage(
        title = stringResource(R.string.convert),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item("categories") {
            // Six categories in the kit's one selection control; they don't fit side by side, so the row scrolls.
            TrailSenseConnectedChoice(
                options = names.mapIndexed { index, name -> TrailSenseChoice(index, name) },
                selected = selectedIndex,
                onSelect = { selectedIndex = it },
                scrollable = true
            )
        }
        item("converter") {
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec)
                },
                label = "converter"
            ) { index ->
                pages[index].Content()
            }
        }
    }
}
