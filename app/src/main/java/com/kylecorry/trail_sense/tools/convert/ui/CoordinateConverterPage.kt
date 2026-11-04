package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Coordinates in the same card as the units: the location you type or paste on top, the same place in the chosen
 * format underneath. The format is the answer's pill; its choices take the keyboard's place.
 */
class CoordinateConverterPage(context: Context) : ConverterPage {

    private val formatService = FormatService.getInstance(context)
    private val formats = BuiltInCoordinateFormat.entries

    @Composable
    override fun Content() {
        var coordinateText by rememberSaveable { mutableStateOf("") }
        var format by rememberSaveable { mutableStateOf(formats.first()) }
        var picking by rememberSaveable { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val clipboard = LocalClipboardManager.current
        val coordinate = remember(coordinateText) { Coordinate.parse(coordinateText) }
        val result = remember(coordinate, format) {
            coordinate?.let { formatService.formatLocation(it, format) }.orEmpty()
        }
        val isInvalid = coordinateText.isNotBlank() && coordinate == null

        // Opening Convert doesn't pop the keyboard; it comes back only after picking a format.
        var settled by remember { mutableStateOf(false) }
        LaunchedEffect(picking) {
            if (picking) {
                focusManager.clearFocus()
                keyboard?.hide()
            } else if (settled) {
                focusRequester.requestFocus()
            }
            settled = true
        }
        BackHandler(enabled = picking) { picking = false }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            ConvertCard(onSwap = null, swapLabel = "") {
                ConvertRow(
                    active = true,
                    picking = false,
                    dimmed = picking,
                    unit = stringResource(R.string.location),
                    onPickUnit = null,
                    onActivate = { picking = false },
                    copyText = null,
                    leaveRoomForSwap = false
                ) {
                    ConvertInput(
                        value = coordinateText,
                        onValueChange = { coordinateText = it },
                        focusRequester = focusRequester,
                        keyboardType = KeyboardType.Text,
                        placeholder = "47.6062, -122.3321",
                        isError = isInvalid,
                        onDone = { keyboard?.hide() }
                    )
                    // Paste lives in the field it fills.
                    IconButton(onClick = { clipboard.getText()?.text?.trim()?.let { coordinateText = it } }) {
                        Icon(
                            Icons.Rounded.ContentPaste,
                            contentDescription = stringResource(R.string.convert_paste),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ConvertRow(
                    active = false,
                    picking = picking,
                    dimmed = false,
                    unit = formatService.formatCoordinateType(format),
                    onPickUnit = { picking = !picking },
                    onActivate = null,
                    copyText = result.ifEmpty { null },
                    leaveRoomForSwap = false
                ) {
                    ConvertValue(result, placeholder = stringResource(R.string.dash))
                }
            }

            if (isInvalid) {
                Text(
                    stringResource(R.string.coordinate_input_invalid_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }

            AnimatedVisibility(visible = picking) {
                ConvertUnitGrid(
                    options = formats.map { ConvertUnitOption(it, formatService.formatCoordinateType(it), null) },
                    selected = format,
                    onPick = {
                        format = it
                        picking = false
                    },
                    columns = 2
                )
            }
        }
    }
}
