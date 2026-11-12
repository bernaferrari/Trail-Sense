package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun ErrorSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().errors }
    val errors = remember { ErrorBannerReason.values().sortedBy { it.id } }
    var values by remember {
        mutableStateOf(errors.associateWith { prefs.canShowError(it) })
    }
    val footer = appString(Res.string.settings_error_banners_footer)

    SettingsList(title = appString(Res.string.error_banners)) {
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
        ErrorBannerReason.NoGPS -> appString(Res.string.gps_unavailable)
        ErrorBannerReason.LocationNotSet -> appString(Res.string.location_not_set)
        ErrorBannerReason.CompassPoor -> appString(Res.string.compass_accuracy)
        ErrorBannerReason.NoCompass -> appString(Res.string.compass_unavailable)
        ErrorBannerReason.GPSTimeout -> appString(Res.string.gps_timeouts)
        ErrorBannerReason.LocationPermissionDenied -> appString(Res.string.location_permission)
    }
}
