package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R

abstract class SimpleConvertFragment<T>(private val defaultFrom: T, private val defaultTo: T) :
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

interface ConverterPage {
    @Composable
    fun Content()
}

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
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.distance_from),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { picker = UnitPicker.From },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(getUnitName(from))
            }

            IconButton(
                onClick = {
                    val previousFrom = from
                    from = to
                    to = previousFrom
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null
                )
            }

            Text(
                text = stringResource(R.string.distance_to),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { picker = UnitPicker.To },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(getUnitName(to))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = result,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    picker?.let { unitPicker ->
        AlertDialog(
            onDismissRequest = { picker = null },
            title = {
                Text(
                    stringResource(
                        if (unitPicker == UnitPicker.From) {
                            R.string.distance_from
                        } else {
                            R.string.distance_to
                        }
                    )
                )
            },
            text = {
                Column {
                    units.forEach { unit ->
                        TextButton(
                            onClick = {
                                if (unitPicker == UnitPicker.From) {
                                    from = unit
                                } else {
                                    to = unit
                                }
                                picker = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = getUnitName(unit),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picker = null }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

private enum class UnitPicker {
    From,
    To
}
