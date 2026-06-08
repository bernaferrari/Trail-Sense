package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat

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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                OutlinedTextField(
                    value = coordinateText,
                    onValueChange = { coordinateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.location)) },
                    isError = coordinateText.isNotBlank() && coordinate == null,
                    supportingText = {
                        if (coordinateText.isNotBlank() && coordinate == null) {
                            Text(stringResource(R.string.coordinate_input_invalid_location))
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.distance_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formatService.formatCoordinateType(format))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = result,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (showPicker) {
            AlertDialog(
                onDismissRequest = { showPicker = false },
                title = { Text(stringResource(R.string.distance_to)) },
                text = {
                    Column {
                        formats.forEach { option ->
                            TextButton(
                                onClick = {
                                    format = option
                                    showPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatService.formatCoordinateType(option),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPicker = false }) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }
    }
}
