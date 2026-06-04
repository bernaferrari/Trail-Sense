package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

class ErrorSettingsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val context = LocalContext.current
        val prefs = remember { ErrorPreferences(context) }
        val errors = remember { ErrorBannerReason.values().sortedBy { it.id } }
        var values by remember {
            mutableStateOf(errors.associateWith { prefs.canShowError(it) })
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("header") {
                    TrailSenseSectionHeader(title = stringResource(R.string.error_banners))
                }
                items(errors, key = { it.id }) { error ->
                    TrailSenseSwitchRow(
                        label = getDisplayName(error),
                        checked = values[error] ?: true,
                        onCheckedChange = { canShow ->
                            prefs.setCanShowError(error, canShow)
                            values = values + (error to canShow)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
            }
        }
    }

    @Composable
    private fun getDisplayName(reason: ErrorBannerReason): String {
        return when (reason) {
            ErrorBannerReason.NoGPS -> stringResource(R.string.gps_unavailable)
            ErrorBannerReason.LocationNotSet -> stringResource(R.string.location_not_set)
            ErrorBannerReason.CompassPoor -> stringResource(R.string.compass_accuracy)
            ErrorBannerReason.NoCompass -> stringResource(R.string.compass_unavailable)
            ErrorBannerReason.GPSTimeout -> stringResource(R.string.gps_timeouts)
        }
    }

}
