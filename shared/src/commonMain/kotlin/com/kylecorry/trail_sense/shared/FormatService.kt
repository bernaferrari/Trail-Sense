package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.time.toJavaSeconds
import com.kylecorry.trail_sense.shared.time.toJavaMinutes
import com.kylecorry.trail_sense.shared.time.toJavaHours
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.PlatformFormats
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.geography.formatting.CoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DecimalDegreesCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DegreesDecimalMinutesCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.DegreesMinutesSecondsCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.MGRSCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.OSGBCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.USNGCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.UTMCoordinateFormat
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Volume
import com.kylecorry.sol.units.VolumeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.domain.Probability
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import com.kylecorry.trail_sense.shared.time.DateNameWidth
import com.kylecorry.trail_sense.shared.time.displayName
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.math.roundToLong

/**
 * Turns values into text for display, in the user's language and with their unit, time and coordinate settings.
 * @param strings the app's strings
 * @param platform the dates, file sizes and clock markers each platform formats natively
 * @param prefs the user's settings
 */
class FormatService(
    private val strings: AppStrings,
    private val platform: PlatformFormats,
    private val prefs: UserPreferences
) {

    fun getWeatherImage(condition: WeatherCondition?): ImageVector {
        return when (condition) {
            WeatherCondition.Clear -> TrailIcons.Sunny
            WeatherCondition.Overcast -> TrailIcons.Cloudy
            WeatherCondition.Precipitation -> TrailIcons.Precipitation
            WeatherCondition.Storm -> TrailIcons.Storm
            WeatherCondition.Wind -> TrailIcons.Wind
            WeatherCondition.Rain -> TrailIcons.LightRain
            WeatherCondition.Snow -> TrailIcons.PrecipitationSnow
            WeatherCondition.Thunderstorm -> TrailIcons.Storm
            null -> TrailIcons.Steady
        }
    }

    fun formatWeather(condition: WeatherCondition?): String {
        return when (condition) {
            WeatherCondition.Clear -> strings.get(Res.string.weather_clear)
            WeatherCondition.Overcast -> strings.get(Res.string.weather_overcast)
            WeatherCondition.Precipitation -> strings.get(Res.string.weather_precipitation)
            WeatherCondition.Storm -> strings.get(Res.string.weather_storm)
            WeatherCondition.Wind -> strings.get(Res.string.weather_wind)
            WeatherCondition.Rain -> strings.get(Res.string.precipitation_rain)
            WeatherCondition.Snow -> strings.get(Res.string.precipitation_snow)
            WeatherCondition.Thunderstorm -> strings.get(Res.string.weather_thunderstorm)
            null -> strings.get(Res.string.weather_no_change)
        }
    }

    fun formatWeatherArrival(arrival: WeatherArrivalTime?): String {
        if (arrival?.isExact != true) {
            return when (arrival?.toRelative(Clock.System.now())) {
                RelativeArrivalTime.Now -> strings.get(Res.string.now)
                RelativeArrivalTime.VerySoon -> strings.get(Res.string.very_soon)
                RelativeArrivalTime.Soon -> strings.get(Res.string.soon)
                RelativeArrivalTime.Later -> strings.get(Res.string.later)
                null -> ""
            }.lowercase()
        }

        val time = arrival.time
        val duration = time - Clock.System.now()

        if (duration < 1.minutes) {
            return strings.get(Res.string.now).lowercase()
        }

        if (duration > 20.hours) {
            return strings.get(Res.string.later).lowercase()
        }

        return strings.get(Res.string.at_time,
            formatTime(time, includeSeconds = false)
        )
    }

    fun formatProbability(probability: Probability): String {
        return when (probability) {
            Probability.Never -> strings.get(Res.string.never)
            Probability.Low -> strings.get(Res.string.low)
            Probability.Moderate -> strings.get(Res.string.moderate)
            Probability.High -> strings.get(Res.string.high)
            Probability.Always -> strings.get(Res.string.always)
        }
    }

    fun formatRelativeDateTime(
        time: Instant,
        abbreviateMonth: Boolean = false,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val local = time.toLocalDateTime(zone)
        return formatRelativeDate(
            local.date,
            abbreviateMonth
        ) + " " + formatTime(local.time, includeSeconds, includeMinutes)
    }

    fun formatRelativeDate(date: LocalDate, abbreviateMonth: Boolean = false): String {
        val now = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return when (date) {
            now -> {
                strings.get(Res.string.today)
            }

            now.plus(1, DateTimeUnit.DAY) -> {
                strings.get(Res.string.tomorrow)
            }

            now.minus(1, DateTimeUnit.DAY) -> {
                strings.get(Res.string.yesterday)
            }

            else -> {
                platform.formatDate(
                    date.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    includeWeekDay = false,
                    includeYear = false,
                    abbreviateMonth = abbreviateMonth
                )
            }
        }
    }

    fun formatTime(
        time: Instant,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        return formatTime(time.toLocalDateTime(zone).time, includeSeconds, includeMinutes)
    }

    fun formatTime(
        time: LocalDateTime,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        return formatTime(time.time, includeSeconds, includeMinutes)
    }

    fun formatTime(
        time: LocalTime,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        val amPm = !prefs.use24HourTime
        val padding = if (prefs.addLeadingZeroToTime) Padding.ZERO else Padding.NONE
        val format = LocalTime.Format {
            if (amPm) amPmHour(padding) else hour(padding)
            if (includeMinutes) {
                char(':')
                minute()
            }
            if (includeSeconds) {
                char(':')
                second()
            }
            if (amPm) {
                char(' ')
                val (am, pm) = platform.amPmMarkers
                amPmMarker(am, pm)
            }
        }
        return time.format(format)
    }

    fun formatMonth(
        month: Month,
        short: Boolean = false
    ): String {
        return month.displayName(if (short) DateNameWidth.Short else DateNameWidth.Full)
    }

    fun formatDayOfWeek(
        dayOfWeek: DayOfWeek,
        short: Boolean = false
    ): String {
        return dayOfWeek.displayName(if (short) DateNameWidth.Short else DateNameWidth.Full)
    }

    fun formatDate(
        date: Instant,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return platform.formatDate(
            date,
            includeWeekDay = includeWeekDay,
            includeYear = true,
            abbreviateMonth = abbreviateMonth
        )
    }

    fun formatDate(
        date: LocalDate,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return formatDate(
            date.atStartOfDayIn(TimeZone.currentSystemDefault()),
            includeWeekDay,
            abbreviateMonth
        )
    }

    fun formatDateTime(
        dateTime: Instant,
        relative: Boolean = false,
        abbreviateMonth: Boolean = false,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val local = dateTime.toLocalDateTime(zone)
        val date = if (relative) formatRelativeDate(
            local.date,
            abbreviateMonth = abbreviateMonth
        ) else formatDate(
            dateTime,
            false,
            abbreviateMonth
        )
        val time = formatTime(local.time, false)
        return "$date $time"
    }

    fun formatTimeSpan(
        start: Instant,
        end: Instant,
        relative: Boolean = false,
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val startLocal = start.toLocalDateTime(zone)
        val endLocal = end.toLocalDateTime(zone)

        val dateFormatFn =
            if (relative) { date: Instant ->
                formatRelativeDate(date.toLocalDateTime(zone).date)
            } else { date: Instant ->
                formatDate(date)
            }

        val startText = "${dateFormatFn(start)} ${formatTime(startLocal.time, false)}"
        val endText = if (startLocal.date == endLocal.date) {
            formatTime(endLocal.time, false)
        } else {
            "${dateFormatFn(end)} ${formatTime(endLocal.time, false)}"
        }
        return strings.get(Res.string.dash_separated_pair, startText, endText)
    }

    fun formatElevation(distance: Distance): String {
        val baseDistance = distance.convertTo(prefs.baseDistanceUnits)
        return formatDistance(baseDistance, Units.getDecimalPlaces(baseDistance.units))
    }

    fun formatDistance(
        distance: Distance,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(distance.value, decimalPlaces, strict)
        return when (distance.units) {
            DistanceUnits.Meters -> strings.get(Res.string.precise_meters_format, formatted)
            DistanceUnits.Kilometers -> strings.get(Res.string.precise_kilometers_format,
                formatted
            )

            DistanceUnits.Feet -> strings.get(Res.string.precise_feet_format, formatted)
            DistanceUnits.Miles -> strings.get(Res.string.precise_miles_format, formatted)
            DistanceUnits.NauticalMiles -> strings.get(Res.string.precise_nautical_miles_format,
                formatted
            )

            DistanceUnits.Inches -> strings.get(Res.string.precise_inches_format, formatted)
            DistanceUnits.Centimeters -> strings.get(Res.string.precise_centimeters_format,
                formatted
            )

            DistanceUnits.Yards -> strings.get(Res.string.yards_format, formatted)
            DistanceUnits.Millimeters -> strings.get(Res.string.millimeters_format,
                formatted
            )

            DistanceUnits.Caliber -> strings.get(Res.string.caliber_format, formatted)
        }
    }

    fun formatTime(
        amount: Float,
        units: TimeUnits,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(amount, decimalPlaces, strict)
        return when (units) {
            TimeUnits.Milliseconds -> strings.get(Res.string.milliseconds_format, formatted)
            TimeUnits.Seconds -> strings.get(Res.string.seconds_format, formatted)
            TimeUnits.Minutes -> strings.get(Res.string.minutes_format, formatted)
            TimeUnits.Hours -> strings.get(Res.string.hours_format, formatted)
            TimeUnits.Days -> strings.get(Res.string.days_format, formatted)
        }
    }

    fun formatVolume(volume: Volume, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(volume.value, decimalPlaces, strict)
        return when (volume.units) {
            VolumeUnits.Liters -> strings.get(Res.string.liter_format, formatted)
            VolumeUnits.Milliliter -> strings.get(Res.string.milliliter_format, formatted)
            VolumeUnits.USCups, VolumeUnits.ImperialCups -> strings.get(Res.string.cup_format,
                formatted
            )

            VolumeUnits.USPints, VolumeUnits.ImperialPints -> strings.get(Res.string.pint_format,
                formatted
            )

            VolumeUnits.USQuarts, VolumeUnits.ImperialQuarts -> strings.get(Res.string.quart_format,
                formatted
            )

            VolumeUnits.USOunces, VolumeUnits.ImperialOunces -> strings.get(Res.string.ounces_volume_format,
                formatted
            )

            VolumeUnits.USGallons, VolumeUnits.ImperialGallons -> strings.get(Res.string.gallon_format,
                formatted
            )

            VolumeUnits.USTeaspoons, VolumeUnits.ImperialTeaspoons -> strings.get(Res.string.teaspooon_format,
                formatted
            )

            VolumeUnits.USTablespoons, VolumeUnits.ImperialTablespoons -> strings.get(Res.string.tablespoon_format,
                formatted
            )
        }
    }

    fun formatWeight(weight: Weight, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(weight.value, decimalPlaces, strict)
        return when (weight.units) {
            WeightUnits.Pounds -> strings.get(Res.string.pounds_format, formatted)
            WeightUnits.Ounces -> strings.get(Res.string.ounces_weight_format, formatted)
            WeightUnits.Kilograms -> strings.get(Res.string.kilograms_format, formatted)
            WeightUnits.Grams -> strings.get(Res.string.grams_format, formatted)
            WeightUnits.Milligrams -> strings.get(Res.string.milligrams_format, formatted)
            WeightUnits.Grains -> strings.get(Res.string.grains_format, formatted)
            WeightUnits.Stone -> strings.get(Res.string.stone_format, formatted)
        }
    }

    fun formatDbm(dbm: Int): String {
        return strings.get(Res.string.dbm_format, dbm.toString())
    }

    fun formatPercentage(percent: Float, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(percent, decimalPlaces, strict)
        return strings.get(Res.string.precise_percent_format, formatted)
    }

    fun formatBatteryHealth(batteryHealth: BatteryHealth): String {
        return when (batteryHealth) {
            BatteryHealth.Cold -> strings.get(Res.string.battery_health_cold)
            BatteryHealth.Dead -> strings.get(Res.string.battery_health_dead)
            BatteryHealth.Good -> strings.get(Res.string.quality_good)
            BatteryHealth.Overheat -> strings.get(Res.string.battery_health_overheat)
            BatteryHealth.OverVoltage -> strings.get(Res.string.battery_health_over_voltage)
            BatteryHealth.Unknown -> strings.get(Res.string.unknown)
        }
    }

    fun formatTemperature(
        temperature: Temperature,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(temperature.value, decimalPlaces, strict)
        return when (temperature.units) {
            TemperatureUnits.Fahrenheit -> strings.get(Res.string.precise_temp_f_format,
                formatted
            )

            TemperatureUnits.Celsius -> strings.get(Res.string.precise_temp_c_format, formatted)
        }
    }

    fun formatDegrees(
        degrees: Float,
        decimalPlaces: Int = 0,
        replace360: Boolean = false
    ): String {
        val formatted = DecimalFormatter.format(degrees.toDouble(), decimalPlaces)
        val finalFormatted = if (replace360) formatted.replace("360", "0") else formatted
        return strings.get(Res.string.degree, finalFormatted)
    }

    fun formatDirection(direction: CompassDirection): String {
        return when (direction) {
            CompassDirection.North -> strings.get(Res.string.direction_north)
            CompassDirection.South -> strings.get(Res.string.direction_south)
            CompassDirection.East -> strings.get(Res.string.direction_east)
            CompassDirection.West -> strings.get(Res.string.direction_west)
            CompassDirection.NorthEast -> strings.get(Res.string.direction_north_east)
            CompassDirection.SouthEast -> strings.get(Res.string.direction_south_east)
            CompassDirection.NorthWest -> strings.get(Res.string.direction_north_west)
            CompassDirection.SouthWest -> strings.get(Res.string.direction_south_west)
        }
    }

    fun formatDays(days: Int): String {
        return strings.plural(Res.plurals.number_days, days, days)
    }

    fun formatDuration(
        duration: Duration,
        short: Boolean = false,
        includeSeconds: Boolean = false
    ): String {
        val hours = duration.toJavaHours()
        val minutes = duration.toJavaMinutes() % 60
        val seconds = duration.toJavaSeconds() % 60

        val h = strings.get(Res.string.duration_hour_format, hours)
        val m = strings.get(Res.string.duration_minute_format, minutes)
        val s = strings.get(Res.string.duration_second_format, seconds)

        val strs = mutableListOf<String>()

        if (hours > 0) {
            strs.add(h)
        }

        if (minutes > 0 && (!short || hours == 0L)) {
            strs.add(m)
        }

        if (!(strs.size > 0 && seconds == 0L) && seconds >= 0 && includeSeconds && (!short || (hours == 0L && minutes == 0L))) {
            strs.add(s)
        }

        if (strs.isEmpty()) {
            strs.add(m)
        }

        return strs.joinToString(" ")
    }

    fun formatAcceleration(acceleration: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(acceleration.toDouble(), decimalPlaces)
        return strings.get(Res.string.acceleration_m_s2_format, formatted)
    }

    fun formatMagneticField(magneticField: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(magneticField.toDouble(), decimalPlaces)
        return strings.get(Res.string.magnetic_field_format_precise, formatted)
    }

    fun formatQuality(quality: Quality): String {
        return when (quality) {
            Quality.Poor -> strings.get(Res.string.quality_poor)
            Quality.Moderate -> strings.get(Res.string.moderate)
            Quality.Good -> strings.get(Res.string.quality_good)
            else -> strings.get(Res.string.unknown)
        }
    }

    fun formatPressure(pressure: Pressure, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val symbol = getPressureUnitString(pressure.units)
        val amt = DecimalFormatter.format(pressure.value, decimalPlaces, strict)
        return strings.get(Res.string.pressure_format, amt, symbol)
    }

    // TODO: Rewrite this
    fun formatSpeed(metersPerSecond: Float): String {
        val distanceUnits = prefs.distanceUnits
        val isMetric = distanceUnits == UserPreferences.DistanceUnits.Meters
        val speedUnits = if (isMetric) DistanceUnits.Kilometers else DistanceUnits.Miles
        val unitsPerSecond = Distance.from(metersPerSecond, DistanceUnits.Meters).convertTo(speedUnits).value
        val formattedSpeed = DecimalFormatter.format((unitsPerSecond * 60 * 60).toDouble(), 1)
        return if (isMetric) {
            strings.get(Res.string.kilometers_per_hour_format, formattedSpeed)
        } else {
            strings.get(Res.string.miles_per_hour_format, formattedSpeed)
        }
    }

    fun formatPace(metersPerSecond: Float): String {
        if (!metersPerSecond.isFinite() || metersPerSecond <= 0f) {
            return strings.get(Res.string.dash)
        }

        val distanceUnit = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            DistanceUnits.Kilometers
        } else {
            DistanceUnits.Miles
        }
        val metersPerUnit = Distance.from(1f, distanceUnit)
            .convertTo(DistanceUnits.Meters)
            .value
        val duration = (metersPerUnit / metersPerSecond).roundToLong().seconds
        val minutes = duration.inWholeMinutes
        val seconds = duration.inWholeSeconds % 60
        val formattedPace = "$minutes:${seconds.toString().padStart(2, '0')}"
        return strings.get(Res.string.slash_separated_pair,
            formattedPace,
            getDistanceUnitName(distanceUnit, short = true)
        )
    }

    val builtInCoordinateFormatMap = mapOf<BuiltInCoordinateFormat, CoordinateFormat>(
        BuiltInCoordinateFormat.DecimalDegrees to DecimalDegreesCoordinateFormat(),
        BuiltInCoordinateFormat.DegreesMinutesSeconds to DegreesMinutesSecondsCoordinateFormat(),
        BuiltInCoordinateFormat.DegreesDecimalMinutes to DegreesDecimalMinutesCoordinateFormat(),
        BuiltInCoordinateFormat.UTM to UTMCoordinateFormat(),
        BuiltInCoordinateFormat.MGRS to MGRSCoordinateFormat(),
        BuiltInCoordinateFormat.OSGB to OSGBCoordinateFormat(),
        BuiltInCoordinateFormat.USNG to USNGCoordinateFormat()
    )

    fun formatLocation(
        location: Coordinate,
        format: BuiltInCoordinateFormat? = null,
        fallbackToDD: Boolean = true
    ): String {
        val formatted =
            builtInCoordinateFormatMap[format ?: prefs.coordinateFormat]?.toString(
                location
            ) ?: "?"
        if (formatted == "?" && fallbackToDD) {
            return builtInCoordinateFormatMap[BuiltInCoordinateFormat.DecimalDegrees]!!.toString(
                location
            )
        }
        return formatted
    }

    fun formatCoordinateType(type: BuiltInCoordinateFormat): String {
        return when (type) {
            BuiltInCoordinateFormat.DecimalDegrees -> strings.get(Res.string.coordinate_format_decimal_degrees)
            BuiltInCoordinateFormat.DegreesDecimalMinutes -> strings.get(Res.string.coordinate_format_degrees_decimal_minutes)
            BuiltInCoordinateFormat.DegreesMinutesSeconds -> strings.get(Res.string.coordinate_format_degrees_minutes_seconds)
            BuiltInCoordinateFormat.UTM -> strings.get(Res.string.coordinate_format_utm)
            BuiltInCoordinateFormat.MGRS -> strings.get(Res.string.coordinate_format_mgrs)
            BuiltInCoordinateFormat.USNG -> strings.get(Res.string.coordinate_format_usng)
            BuiltInCoordinateFormat.OSGB -> strings.get(Res.string.coordinate_format_osgb)
        }
    }

    fun formatElectricalCapacity(capacity: Float): String {
        return strings.get(Res.string.battery_capacity_format,
            DecimalFormatter.format(capacity, 0)
        )
    }

    fun formatCurrent(current: Float, decimalPlaces: Int = 0): String {
        return strings.get(Res.string.current_format,
            DecimalFormatter.format(current, decimalPlaces)
        )
    }

    fun formatLux(lux: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(lux.toDouble(), decimalPlaces)
        return strings.get(Res.string.lux_format, formatted)
    }

    fun formatCandela(candela: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(candela.toDouble(), decimalPlaces)
        return strings.get(Res.string.candela_format, formatted)
    }

    fun formatSolarEnergy(energy: Float, decimalPlaces: Int = 1, strict: Boolean = false): String {
        val formatted = DecimalFormatter.format(energy, decimalPlaces, strict)
        return strings.get(Res.string.kwh_per_meter_squared_format, formatted)
    }

    fun formatFileSize(bytes: Long, short: Boolean = true): String {
        return platform.formatFileSize(bytes, short)
    }

    fun formatOfflineMapFileTypeName(type: VectorMapFileType): String {
        return when (type) {
            VectorMapFileType.Mapsforge -> strings.get(Res.string.mapsforge)
        }
    }

    fun sortDistanceUnits(
        units: List<DistanceUnits>,
        metric: Boolean = prefs.baseDistanceUnits == DistanceUnits.Meters
    ): List<DistanceUnits> {
        // TODO: Secondary sort by size
        val metricUnits =
            listOf(DistanceUnits.Centimeters, DistanceUnits.Meters, DistanceUnits.Kilometers)
        return units.sortedBy {
            if (metric) {
                if (metricUnits.contains(it)) 0 else 1
            } else {
                if (metricUnits.contains(it)) 1 else 0
            }
        }
    }

    fun sortWeightUnits(
        units: List<WeightUnits>,
        metric: Boolean = prefs.weightUnits == WeightUnits.Kilograms
    ): List<WeightUnits> {
        // TODO: Secondary sort by size
        val metricUnits =
            listOf(WeightUnits.Grams, WeightUnits.Kilograms)
        return units.sortedBy {
            if (metric) {
                if (metricUnits.contains(it)) 0 else 1
            } else {
                if (metricUnits.contains(it)) 1 else 0
            }
        }
    }

    fun getWeightUnitName(unit: WeightUnits, short: Boolean = false): String {
        if (short) {
            return when (unit) {
                WeightUnits.Pounds -> strings.get(Res.string.pounds_format, "")
                WeightUnits.Ounces -> strings.get(Res.string.ounces_weight_format, "")
                WeightUnits.Kilograms -> strings.get(Res.string.kilograms_format, "")
                WeightUnits.Grams -> strings.get(Res.string.grams_format, "")
                WeightUnits.Milligrams -> strings.get(Res.string.milligrams_format, "")
                WeightUnits.Grains -> strings.get(Res.string.grains_format, "")
                WeightUnits.Stone -> strings.get(Res.string.stone_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            WeightUnits.Pounds -> strings.get(Res.string.pounds)
            WeightUnits.Ounces -> strings.get(Res.string.ounces_weight)
            WeightUnits.Kilograms -> strings.get(Res.string.kilograms)
            WeightUnits.Grams -> strings.get(Res.string.grams)
            WeightUnits.Milligrams -> strings.get(Res.string.milligrams)
            WeightUnits.Grains -> strings.get(Res.string.grains)
            WeightUnits.Stone -> strings.get(Res.string.stone)
        }
    }

    fun getBulletSpeedUnitName(unit: DistanceUnits, short: Boolean = false): String {
        // Only supports Feet and Meters
        if (short) {
            return when (unit) {
                DistanceUnits.Feet -> strings.get(Res.string.speed_format_feet_per_second, "")
                DistanceUnits.Meters -> strings.get(Res.string.speed_format_meters_per_second,
                    ""
                )

                else -> ""
            }.replace(" ", "")
        }

        return when (unit) {
            DistanceUnits.Feet -> strings.get(Res.string.feet_per_second)
            DistanceUnits.Meters -> strings.get(Res.string.meters_per_second)
            else -> ""
        }
    }

    fun getPressureUnitName(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> strings.get(Res.string.units_hpa)
            PressureUnits.Mbar -> strings.get(Res.string.units_mbar)
            PressureUnits.Inhg -> strings.get(Res.string.units_inhg)
            PressureUnits.Psi -> strings.get(Res.string.units_psi)
            PressureUnits.MmHg -> strings.get(Res.string.units_mmhg)
        }
    }

    fun getTemperatureUnitName(unit: TemperatureUnits, short: Boolean = false): String {
        return if (short) {
            when (unit) {
                TemperatureUnits.Fahrenheit -> strings.get(Res.string.temp_f_short)
                TemperatureUnits.Celsius -> strings.get(Res.string.temp_c_short)
            }
        } else {
            when (unit) {
                TemperatureUnits.Fahrenheit -> strings.get(Res.string.fahrenheit)
                TemperatureUnits.Celsius -> strings.get(Res.string.celsius)
            }
        }
    }

    fun getDistanceUnitName(unit: DistanceUnits, short: Boolean = false): String {
        if (short) {
            return when (unit) {
                DistanceUnits.Meters -> strings.get(Res.string.precise_meters_format, "")
                DistanceUnits.Kilometers -> strings.get(Res.string.precise_kilometers_format,
                    ""
                )

                DistanceUnits.Feet -> strings.get(Res.string.precise_feet_format, "")
                DistanceUnits.Miles -> strings.get(Res.string.precise_miles_format, "")
                DistanceUnits.NauticalMiles -> strings.get(Res.string.precise_nautical_miles_format,
                    ""
                )

                DistanceUnits.Centimeters -> strings.get(Res.string.precise_centimeters_format,
                    ""
                )

                DistanceUnits.Inches -> strings.get(Res.string.precise_inches_format, "")
                DistanceUnits.Yards -> strings.get(Res.string.yards_format, "")
                DistanceUnits.Millimeters -> strings.get(Res.string.millimeters_format,
                    ""
                )

                DistanceUnits.Caliber -> strings.get(Res.string.caliber_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            DistanceUnits.Meters -> strings.get(Res.string.unit_meters)
            DistanceUnits.Kilometers -> strings.get(Res.string.unit_kilometers)
            DistanceUnits.Feet -> strings.get(Res.string.unit_feet)
            DistanceUnits.Miles -> strings.get(Res.string.unit_miles)
            DistanceUnits.NauticalMiles -> strings.get(Res.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> strings.get(Res.string.unit_centimeters)
            DistanceUnits.Inches -> strings.get(Res.string.unit_inches)
            DistanceUnits.Yards -> strings.get(Res.string.unit_yards)
            DistanceUnits.Millimeters -> strings.get(Res.string.unit_millimeters)
            DistanceUnits.Caliber -> strings.get(Res.string.unit_caliber)
        }
    }

    fun getSpeedUnitName(
        distanceUnit: DistanceUnits,
        timeUnit: TimeUnits,
        short: Boolean = false
    ): String {
        return when (distanceUnit to timeUnit) {
            DistanceUnits.Kilometers to TimeUnits.Hours -> strings.get(Res.string.kilometers_per_hour_format,
                ""
            ).replace(" ", "")

            DistanceUnits.Miles to TimeUnits.Hours -> strings.get(Res.string.miles_per_hour_format,
                ""
            ).replace(" ", "")

            DistanceUnits.Meters to TimeUnits.Seconds -> strings.get(Res.string.speed_format_meters_per_second,
                ""
            ).replace(" ", "")

            else -> throw IllegalArgumentException("Unsupported speed unit: $distanceUnit / $timeUnit")
        }
    }

    fun formatCellNetwork(cellType: CellNetwork?): String {
        return when (cellType) {
            CellNetwork.Nr -> strings.get(Res.string.network_5g)
            CellNetwork.Lte -> strings.get(Res.string.network_4g)
            CellNetwork.Cdma, CellNetwork.Gsm -> strings.get(Res.string.network_2g)
            CellNetwork.Wcdma -> strings.get(Res.string.network_3g)
            else -> strings.get(Res.string.network_no_signal)
        }
    }

    fun formatMoonPhase(phase: MoonTruePhase): String {
        return strings.get(
            when (phase) {
                MoonTruePhase.FirstQuarter -> Res.string.first_quarter
                MoonTruePhase.Full -> Res.string.full_moon
                MoonTruePhase.ThirdQuarter -> Res.string.third_quarter
                MoonTruePhase.New -> Res.string.new_moon
                MoonTruePhase.WaningCrescent -> Res.string.waning_crescent
                MoonTruePhase.WaningGibbous -> Res.string.waning_gibbous
                MoonTruePhase.WaxingCrescent -> Res.string.waxing_crescent
                MoonTruePhase.WaxingGibbous -> Res.string.waxing_gibbous
            }
        )
    }

    fun formatSeason(season: Season): String {
        return when (season) {
            Season.Winter -> strings.get(Res.string.season_winter)
            Season.Spring -> strings.get(Res.string.season_spring)
            Season.Summer -> strings.get(Res.string.season_summer)
            Season.Fall -> strings.get(Res.string.season_fall)
        }
    }


    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> strings.get(Res.string.units_hpa)
            PressureUnits.Mbar -> strings.get(Res.string.units_mbar)
            PressureUnits.Inhg -> strings.get(Res.string.units_inhg_short)
            PressureUnits.Psi -> strings.get(Res.string.units_psi)
            PressureUnits.MmHg -> strings.get(Res.string.units_mmhg_short)
        }
    }

    fun formatPrecipitation(precipitation: Precipitation): String {
        return when (precipitation) {
            Precipitation.Rain -> strings.get(Res.string.precipitation_rain)
            Precipitation.Drizzle -> strings.get(Res.string.precipitation_drizzle)
            Precipitation.Snow -> strings.get(Res.string.precipitation_snow)
            Precipitation.SnowPellets -> strings.get(Res.string.precipitation_snow_pellets)
            Precipitation.Hail -> strings.get(Res.string.precipitation_hail)
            Precipitation.SmallHail -> strings.get(Res.string.precipitation_small_hail)
            Precipitation.IcePellets -> strings.get(Res.string.precipitation_ice_pellets)
            Precipitation.SnowGrains -> strings.get(Res.string.precipitation_snow_grains)
            Precipitation.Lightning -> strings.get(Res.string.lightning)
        }
    }

    fun formatHikingDifficulty(difficulty: HikingDifficulty): String {
        return when (difficulty) {
            HikingDifficulty.Easy -> strings.get(Res.string.easy)
            HikingDifficulty.Moderate -> strings.get(Res.string.moderate)
            else -> strings.get(Res.string.hard)
        }
    }

    fun formatMapProjection(projection: MapProjectionType): String {
        return when (projection) {
            MapProjectionType.Mercator -> strings.get(Res.string.map_projection_mercator)
            MapProjectionType.CylindricalEquidistant -> strings.get(Res.string.map_projection_equidistant)
        }
    }

    fun formatBooleanYesNo(value: Boolean): String {
        return if (value) strings.get(Res.string.yes) else strings.get(Res.string.no)
    }

    fun join(vararg strings: String?, separator: Separator = Separator.Comma): String {
        val separatorString = when (separator) {
            Separator.Dot -> " • "
            Separator.Comma -> ", "
            Separator.Space -> " "
            Separator.Dash -> " - "
            Separator.NewLine -> "\n"
        }
        return strings.filterNotNull().joinToString(separatorString)
    }

    fun getCompassReferenceIcon(isTrueNorth: Boolean = prefs.compass.useTrueNorth): ImageVector {
        return if (isTrueNorth) {
            TrailIcons.Star
        } else {
            TrailIcons.ToolMetalDetector
        }
    }

    enum class Separator {
        Dot,
        Comma,
        Space,
        Dash,
        NewLine
    }

    companion object {
        /** The app's instance. */
        fun getInstance(): FormatService = getAppService()
    }

}
