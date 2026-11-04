package com.kylecorry.trail_sense.shared.views.compose

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

@Composable
fun BearingInput(
    bearing: Float?,
    trueNorth: Boolean,
    compassBearingText: String,
    hasCompass: Boolean,
    onBearingChange: (Float?, Boolean) -> Unit,
    onCompassClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    var showManualEntry by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(tag?.let { Modifier.testTag(it) } ?: Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasCompass) {
            CompassAutofillButton(
                compassBearingText = compassBearingText,
                onClick = onCompassClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bearing?.let { formatter.formatDegrees(it, replace360 = true) }
                    ?: stringResource(R.string.direction_not_set),
                modifier = Modifier.testTag("bearing"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (bearing != null) {
                NorthReferenceBadge(
                    trueNorth = trueNorth,
                    modifier = Modifier.testTag("north_reference_badge")
                )
            }
            Row(
                modifier = Modifier
                    .clickable { showManualEntry = true }
                    .padding(top = 2.dp)
                    .testTag("manual_bearing"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.enter_manually),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = TrailIcons.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (bearing != null) {
            IconButton(
                onClick = { onBearingChange(null, false) },
                modifier = Modifier.testTag("clear_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Cancel,
                    contentDescription = stringResource(android.R.string.cancel)
                )
            }
        }

        if (hasCompass) {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.testTag("camera_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Camera,
                    contentDescription = stringResource(R.string.camera)
                )
            }
        }
    }

    if (showManualEntry) {
        ManualBearingDialog(
            initialBearing = bearing,
            initialTrueNorth = trueNorth,
            onDismiss = { showManualEntry = false },
            onConfirm = { selectedBearing, selectedTrueNorth ->
                showManualEntry = false
                onBearingChange(selectedBearing, selectedTrueNorth)
            }
        )
    }
}

@Composable
private fun CompassAutofillButton(
    compassBearingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("compass_autofill_holder"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("compass_btn")
        ) {
            Icon(
                imageVector = TrailIcons.CompassIcon,
                contentDescription = stringResource(R.string.direction)
            )
        }
        Text(
            text = compassBearingText,
            modifier = Modifier.testTag("compass_bearing"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NorthReferenceBadge(
    trueNorth: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    Row(
        modifier = modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = rememberVectorPainter(formatter.getCompassReferenceIcon(trueNorth)),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(if (trueNorth) R.string.true_north else R.string.magnetic_north),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ManualBearingDialog(
    initialBearing: Float?,
    initialTrueNorth: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float?, Boolean) -> Unit
) {
    var bearingText by remember(initialBearing) {
        mutableStateOf(initialBearing?.let { DecimalFormatter.format(it, 1) } ?: "")
    }
    var trueNorth by remember(initialTrueNorth) { mutableStateOf(initialTrueNorth) }
    val parsedBearing = bearingText.toFloatOrNull()?.let { Bearing.getBearing(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.direction)) },
        text = {
            Column {
                OutlinedTextField(
                    value = bearingText,
                    onValueChange = { bearingText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bearing"),
                    label = { Text(stringResource(R.string.bearing)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.lg)
                        .testTag("true_north"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.true_north),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = trueNorth,
                        onCheckedChange = { trueNorth = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsedBearing, trueNorth) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BearingInputPreview() {
    TrailSenseTheme {
        BearingInput(
            bearing = 135f,
            trueNorth = false,
            compassBearingText = "120 deg",
            hasCompass = true,
            onBearingChange = { _, _ -> },
            onCompassClick = {},
            onCameraClick = {}
        )
    }
}
