package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

abstract class SimpleConverterPage<T>(private val defaultFrom: T, private val defaultTo: T) :
    ConverterPage {

    abstract val units: List<T>

    @Composable
    override fun Content() {
        SimpleConvertScreen(
            units = units,
            defaultFrom = defaultFrom,
            defaultTo = defaultTo,
            getUnitName = ::getUnitName,
            convert = ::convert
        )
    }

    abstract fun convert(amount: Float, from: T, to: T): String

    abstract fun getUnitName(unit: T): String
}

/** One converter, drawn as a plain column: the page around it owns scrolling and gutters. */
interface ConverterPage {
    @Composable
    fun Content()
}

/**
 * The result leads, so it stays in view above the keyboard while you type; the amount and its unit are one field,
 * and the target unit sits right under it with the swap button between the two.
 */
@Composable
fun <T> SimpleConvertScreen(
    units: List<T>,
    defaultFrom: T,
    defaultTo: T,
    getUnitName: (T) -> String,
    convert: (Float, T, T) -> String
) {
    var amountText by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(defaultFrom) }
    var to by remember { mutableStateOf(defaultTo) }
    var picker by remember { mutableStateOf<UnitPicker?>(null) }

    val amount = amountText.toFloatOrNull() ?: 0f
    val result = remember(amount, from, to) {
        convert(amount, from, to)
    }

    ConvertPair(
        value = amountText,
        onValueChange = { amountText = it },
        fromLabel = stringResource(R.string.distance_from),
        fromUnit = getUnitName(from),
        onFromUnitClick = { picker = UnitPicker.From },
        result = if (amountText.isBlank()) "" else result,
        toLabel = stringResource(R.string.distance_to),
        toUnit = getUnitName(to),
        onToUnitClick = { picker = UnitPicker.To },
        placeholder = stringResource(R.string.places_convert_hint),
        onSwap = {
            val previousFrom = from
            from = to
            to = previousFrom
        },
        swapLabel = stringResource(R.string.convert_swap_units)
    )

    picker?.let { unitPicker ->
        TrailSenseOptionsDialog(
            title = stringResource(
                if (unitPicker == UnitPicker.From) R.string.distance_from else R.string.distance_to
            ),
            options = units.map { getUnitName(it) to it },
            selected = if (unitPicker == UnitPicker.From) from else to,
            onSelect = { unit ->
                if (unitPicker == UnitPicker.From) {
                    from = unit
                } else {
                    to = unit
                }
                picker = null
            },
            onDismiss = { picker = null }
        )
    }
}

/** The converted value: the one thing that matters on a converter, as emphasised type on a quiet surface. */
@Composable
internal fun ConvertResult(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text.ifBlank { stringResource(R.string.dash) },
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xxl)
        )
    }
}

private enum class UnitPicker {
    From,
    To
}
