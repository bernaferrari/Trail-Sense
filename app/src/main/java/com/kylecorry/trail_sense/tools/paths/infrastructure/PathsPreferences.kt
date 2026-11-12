package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.sense.location.GPSPowerUsage
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.GPSPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.paths.ui.PathSortMethod
import kotlin.time.Duration

class PathsPreferences(private val context: Context) : IPathPreferences {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val gps by lazy { GPSPreferences(context) }

    var pathSort: PathSortMethod by IntEnumPreference(
        cache,
        PreferenceKeys.pathSort,
        PathSortMethod.entries.associateBy { it.id.toInt() },
        PathSortMethod.MostRecent
    )

    var backtrackEnabled by BooleanPreference(
        cache,
        PreferenceKeys.backtrackEnabled,
        false
    )

    var backtrackSaveCellHistory by BooleanPreference(
        cache,
        PreferenceKeys.backtrackSaveCell,
        true
    )

    override val keepBacktrackPathOnStop by BooleanPreference(
        cache,
        PreferenceKeys.backtrackKeepOnStop,
        true
    )

    val backtrackKeepDeviceAwake by BooleanPreference(
        cache,
        PreferenceKeys.backtrackKeepAwake,
        false
    )

    private val backtrackGPSPowerMode by StringEnumPreference(
        cache,
        PreferenceKeys.backtrackGpsPowerUsage,
        GPSPowerMode.entries.associateBy { it.id.toString() },
        GPSPowerMode.Inherit
    )

    val backtrackGPSPowerUsage: GPSPowerUsage
        get() = backtrackGPSPowerMode.powerUsage ?: gps.powerMode.powerUsage ?: GPSPowerUsage.High

    var backtrackRecordFrequency: Duration
        get() = cache.getDuration(PreferenceKeys.backtrackFrequency)
            ?: 1.minutes
        set(value) {
            cache.putDuration(PreferenceKeys.backtrackFrequency, value)
        }

    var backtrackMinimumDistance: Distance
        get() {
            val meters = cache.getFloat(PreferenceKeys.backtrackMinDistance)
                ?: DEFAULT_BACKTRACK_MIN_DISTANCE_METERS
            return Distance.meters(meters)
        }
        set(value) {
            cache.putFloat(
                PreferenceKeys.backtrackMinDistance,
                value.meters().value
            )
        }

    var defaultPathColor: AppColor
        get() {
            val id = cache.getLong(PreferenceKeys.backtrackPathColor)
            return AppColor.values().firstOrNull { it.id == id } ?: AppColor.Gray
        }
        set(value) {
            cache.putLong(PreferenceKeys.backtrackPathColor, value.id)
        }

    private val backtrackPathLineStyle: LineStyle
        get() {
            return when (cache.getString(PreferenceKeys.backtrackPathStyle)) {
                "solid" -> LineStyle.Solid
                "arrow" -> LineStyle.Arrow
                "dashed" -> LineStyle.Dashed
                "square" -> LineStyle.Square
                "diamond" -> LineStyle.Diamond
                "cross" -> LineStyle.Cross
                else -> LineStyle.Dotted
            }
        }

    override val defaultPathStyle: PathStyle
        get() = PathStyle(
            backtrackPathLineStyle,
            PathPointColoringStyle.None,
            defaultPathColor.color,
            true
        )

    override var backtrackHistory: Duration
        get() {
            val days = cache.getInt(PreferenceKeys.backtrackHistoryDays) ?: 7
            return (days.toLong()).days
        }
        set(value) {
            val d = value.inWholeDays.toInt()
            cache.putInt(
                PreferenceKeys.backtrackHistoryDays,
                if (d > 0) d else 1
            )
        }

    override val simplifyPathOnImport by BooleanPreference(
        cache,
        PreferenceKeys.autoSimplifyPaths,
        true
    )

    override val useFastPathRendering by BooleanPreference(
        cache,
        PreferenceKeys.fastPathRendering,
        false
    )

    val elevationSmoothingScale: Int
        get() = context.resources.getInteger(R.integer.elevation_smoothing_scale)

    override val elevationSmoothing: Float
        get() {
            val scale = elevationSmoothingScale
            val scaled = cache.getInt(PreferenceKeys.elevationSmoothing)
                ?: (HikingService.DEFAULT_ELEVATION_SMOOTHING * scale).toInt()
            return scaled / scale.toFloat()
        }

    companion object {
        const val DEFAULT_BACKTRACK_MIN_DISTANCE_METERS = 10f
    }
}
