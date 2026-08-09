package com.kylecorry.trail_sense.tools.diagnostics.status

import android.content.Context
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import java.time.Duration
import java.time.Instant

class GpsStatusBadgeProvider(private val gps: ISatelliteGPS, private val context: Context) :
    StatusBadgeProvider {

    private val formatter = FormatService.getInstance(context)
    private val prefs = UserPreferences(context)

    override fun getBadge(): StatusBadge {
        return StatusBadge(getName(), getColor(), R.drawable.satellite, isHealthy())
    }

    private fun isHealthy(): Boolean {
        if (gps is OverrideGPS) return true
        if (gps is InactiveGPS || !GPS.isAvailable(context)) return false
        // Searching and stale readings are transient states already communicated by the readout.
        // Keep the persistent badge for conditions the user can act on.
        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) return true
        if (!gps.hasValidReading || (prefs.requiresSatellites && (gps.satellites ?: 0) < 4)) return true
        if (gps is CustomGPS && gps.isTimedOut) return true
        return gps.quality != Quality.Poor
    }

    @ColorInt
    private fun getColor(): Int {
        if (gps is OverrideGPS) {
            return AppColor.Green.color
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return AppColor.Red.color
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return AppColor.Yellow.color
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && (gps.satellites
                ?: 0) < 4) || (gps is CustomGPS && gps.isTimedOut)
        ) {
            return AppColor.Yellow.color
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getName(): String {
        if (gps is OverrideGPS) {
            return context.getString(R.string.gps_user)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return context.getString(R.string.unavailable)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return context.getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && (gps.satellites
                ?: 0) < 4) || (gps is CustomGPS && gps.isTimedOut)
        ) {
            return context.getString(R.string.gps_searching)
        }

        return formatter.formatQuality(gps.quality)
    }
}
