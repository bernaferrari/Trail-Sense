package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun CameraSettingsToolScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).camera }
    val projectionNames = context.projectionTypeNames()
    val projectionTypes = projectionNames.keys.toList()
    var projectionType by remember { mutableStateOf(prefs.projectionType) }
    val title = stringResource(R.string.projection_type)
    SettingsList(title = stringResource(R.string.camera)) {
        trailSenseSection("projection") {
            row { position ->
                SettingsPickerRow(
                    label = title,
                    selected = projectionNames.getValue(projectionType),
                    onClick = {
                        Pickers.item(
                            context,
                            title,
                            projectionNames.values.toList(),
                            projectionTypes.indexOf(projectionType)
                        ) {
                            if (it != null) {
                                projectionType = projectionTypes[it]
                                prefs.projectionType = projectionTypes[it]
                            }
                        }
                    },
                    position = position
                )
            }
        }
    }
}

private fun Context.projectionTypeNames(): Map<CameraPreferences.ProjectionType, String> {
    return mapOf(
        CameraPreferences.ProjectionType.EstimatedIntrinsics to
                "${getString(R.string.projection_estimated_intrinsics)} (${
                    getString(R.string.default_string)
                })",
        CameraPreferences.ProjectionType.ManufacturerIntrinsics to
                getString(R.string.projection_manufacturer_intrinsics),
        CameraPreferences.ProjectionType.Perspective to getString(R.string.projection_perspective),
        CameraPreferences.ProjectionType.Linear to getString(R.string.projection_linear)
    )
}
