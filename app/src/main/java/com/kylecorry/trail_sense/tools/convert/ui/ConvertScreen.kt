@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoiceDefaults
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseChoiceColors
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.trailSenseImePadding

/** Distance, in [ConvertScreen]'s category order. */
private const val DEFAULT_CATEGORY = 1

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
 * Convert as Calcula does it: the categories in a sideways row, then the converter card, and the system keyboard (or
 * the units, while you pick one) underneath. Nothing else on the page, so the answer stays above the keyboard.
 */
@Composable
fun ConverterTabs(
    pages: List<ConverterPage>,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    // One question: what is this amount in the other unit?
    // Opens on Distance, the everyday conversion.
    var selectedIndex by rememberSaveable { mutableIntStateOf(DEFAULT_CATEGORY) }
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    // Each converter keeps what you typed and the units you chose while you look at another one.
    val saved = rememberSaveableStateHolder()

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.convert),
        largeTitle = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Only the part of an open keyboard that covers this page (the bar below already takes the
                // rest); nothing while it is closed. Scrolls, so the card is never cropped.
                .trailSenseImePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            ConvertCategories(
                names = names,
                selected = selectedIndex,
                onSelect = { selectedIndex = it }
            )
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) },
                label = "converter",
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
            ) { index ->
                saved.SaveableStateProvider(index) {
                    pages[index].Content()
                }
            }
        }
    }
}

/** The categories as Calcula's sideways row of toggle buttons, scrolled so the chosen one stays in view. */
@Composable
private fun ConvertCategories(names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState()
    // Only scroll when the chosen category is not fully in view, so the row opens at its start with every chip
    // whole rather than cut at the left edge.
    LaunchedEffect(selected) {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == selected }
        val fullyVisible = if (item == null) {
            info.visibleItemsInfo.isEmpty() && selected < 3
        } else {
            item.offset >= info.viewportStartOffset && item.offset + item.size <= info.viewportEndOffset
        }
        if (!fullyVisible) state.animateScrollToItem(selected, scrollOffset = -120)
    }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        itemsIndexed(names) { index, name ->
            ToggleButton(
                checked = index == selected,
                onCheckedChange = { if (it) onSelect(index) },
                colors = trailSenseChoiceColors(),
                modifier = Modifier
                    .heightIn(min = TrailSenseChoiceDefaults.height)
                    .semantics { role = Role.RadioButton }
            ) {
                Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}
