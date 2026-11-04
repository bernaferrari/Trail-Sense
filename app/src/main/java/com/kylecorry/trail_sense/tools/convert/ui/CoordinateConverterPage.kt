package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class CoordinateConverterPage(context: Context) : ConverterPage {

    private val formatService = FormatService.getInstance(context)
    private val formats = BuiltInCoordinateFormat.entries

    @Composable
    override fun Content() {
        var coordinateText by remember { mutableStateOf("") }
        var format by remember { mutableStateOf(formats.first()) }
        var showPicker by remember { mutableStateOf(false) }
        val coordinate = remember(coordinateText) { Coordinate.parse(coordinateText) }
        val result = remember(coordinate, format) {
            coordinate?.let { formatService.formatLocation(it, format) }.orEmpty()
        }
        val isInvalid = coordinateText.isNotBlank() && coordinate == null

        ConvertPair(
            value = coordinateText,
            onValueChange = { coordinateText = it },
            fromLabel = stringResource(R.string.location),
            fromUnit = null,
            onFromUnitClick = null,
            result = result,
            toLabel = stringResource(R.string.distance_to),
            toUnit = formatService.formatCoordinateType(format),
            onToUnitClick = { showPicker = true },
            placeholder = stringResource(R.string.places_convert_coordinate_hint),
            emptyValue = "--°, --°",
            keyboardType = KeyboardType.Text,
            isError = isInvalid,
            errorText = stringResource(R.string.coordinate_input_invalid_location)
        )

        if (showPicker) {
            TrailSenseOptionsDialog(
                title = stringResource(R.string.distance_to),
                options = formats.map { formatService.formatCoordinateType(it) to it },
                selected = format,
                onSelect = {
                    format = it
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }
    }
}
