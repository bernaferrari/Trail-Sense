package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun CameraSettingsToolScreen() {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val prefs = remember { getAppService<UserPreferences>().camera }
    val projectionNames = projectionTypeNames(strings)
    val projectionTypes = projectionNames.keys.toList()
    var projectionType by remember { mutableStateOf(prefs.projectionType) }
    val title = appString(Res.string.projection_type)
    SettingsList(title = appString(Res.string.camera)) {
        trailSenseSection("projection") {
            row { position ->
                SettingsPickerRow(
                    label = title,
                    selected = projectionNames.getValue(projectionType),
                    onClick = {
                        Pickers.item(
                            host,
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

private fun projectionTypeNames(strings: AppStrings): Map<CameraPreferences.ProjectionType, String> {
    return mapOf(
        CameraPreferences.ProjectionType.EstimatedIntrinsics to
                "${strings.get(Res.string.projection_estimated_intrinsics)} (${
                    strings.get(Res.string.default_string)
                })",
        CameraPreferences.ProjectionType.ManufacturerIntrinsics to
                strings.get(Res.string.projection_manufacturer_intrinsics),
        CameraPreferences.ProjectionType.Perspective to strings.get(Res.string.projection_perspective),
        CameraPreferences.ProjectionType.Linear to strings.get(Res.string.projection_linear)
    )
}
