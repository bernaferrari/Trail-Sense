package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.activity.compose.LocalActivity

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun PrivacySettingsToolScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navigator = LocalNavigator.current
    val prefs = remember { UserPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLocationMocked by remember { mutableStateOf(context.isLocationMocked(prefs)) }
    var isScreenshotProtectionOn by remember {
        mutableStateOf(prefs.privacy.isScreenshotProtectionOn)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationMocked = context.isLocationMocked(prefs)
                isScreenshotProtectionOn = prefs.privacy.isScreenshotProtectionOn
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsList(title = stringResource(R.string.privacy)) {
        trailSenseSection("privacy") {
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.location),
                    summary = if (isLocationMocked) {
                        stringResource(R.string.location_mocked)
                    } else {
                        stringResource(R.string.location_not_mocked)
                    },
                    onClick = {
                        navigator.navigate(
                            Route.CalibrateGps
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_privacy_screenshot_protection_title),
                    checked = isScreenshotProtectionOn,
                    onCheckedChange = {
                        isScreenshotProtectionOn = it
                        prefs.privacy.isScreenshotProtectionOn = it
                        activity?.window?.let { window -> Screen.setAllowScreenshots(window, !it) }
                    },
                    position = position
                )
            }
        }
    }
}

private fun Context.isLocationMocked(prefs: UserPreferences): Boolean {
    return prefs.gps.locationSource == GPSLocationSource.Manual ||
        !SensorService(this).hasLocationPermission()
}
