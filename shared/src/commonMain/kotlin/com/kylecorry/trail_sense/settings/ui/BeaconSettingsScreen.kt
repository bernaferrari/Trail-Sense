package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun BeaconSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>() }
    var defaultColor by remember {
        mutableStateOf(prefs.beacons.defaultBeaconColor)
    }
    var showDefaultColorPicker by remember { mutableStateOf(false) }
    val defaultColorTitle = appString(Res.string.pref_default_beacon_color_title)
    SettingsList(title = appString(Res.string.beacons)) {
        trailSenseSection("color") {
            row { position ->
                SettingsActionRow(
                    label = defaultColorTitle,
                    onClick = {
                        showDefaultColorPicker = true
                    },
                    trailing = {
                        Box(
                            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        Color(defaultColor.color),
                                        CircleShape
                                    )
                            )
                        }
                    },
                    position = position
                )
            }
        }
    }

    if (showDefaultColorPicker) {
        ColorPickerDialog(
            title = defaultColorTitle,
            selectedColor = defaultColor,
            onColorPicked = {
                if (it != null) {
                    prefs.beacons.defaultBeaconColor = it
                    defaultColor = it
                }
                showDefaultColorPicker = false
            },
            onDismiss = {
                showDefaultColorPicker = false
            }
        )
    }
}
