package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun ErrorSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { ErrorPreferences(host.requireContext()) }
    val errors = remember { ErrorBannerReason.values().sortedBy { it.id } }
    var values by remember {
        mutableStateOf(errors.associateWith { prefs.canShowError(it) })
    }
    val footer = stringResource(R.string.settings_error_banners_footer)

    SettingsList(host, title = stringResource(R.string.error_banners)) {
        trailSenseSection("errors", footer = footer) {
            errors.forEach { error ->
                row { position ->
                    SettingsSwitchRow(
                        label = getErrorDisplayName(error),
                        checked = values[error] ?: true,
                        onCheckedChange = { canShow ->
                            prefs.setCanShowError(error, canShow)
                            values = values + (error to canShow)
                        },
                        position = position
                    )
                }
            }
        }
    }
}

@Composable
private fun getErrorDisplayName(reason: ErrorBannerReason): String {
    return when (reason) {
        ErrorBannerReason.NoGPS -> stringResource(R.string.gps_unavailable)
        ErrorBannerReason.LocationNotSet -> stringResource(R.string.location_not_set)
        ErrorBannerReason.CompassPoor -> stringResource(R.string.compass_accuracy)
        ErrorBannerReason.NoCompass -> stringResource(R.string.compass_unavailable)
        ErrorBannerReason.GPSTimeout -> stringResource(R.string.gps_timeouts)
        ErrorBannerReason.LocationPermissionDenied -> stringResource(R.string.location_permission)
    }
}
