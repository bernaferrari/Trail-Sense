package com.kylecorry.trail_sense.tools.astronomy.ui.items

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_rise
import com.kylecorry.trail_sense.shared.generated.resources.astronomy_set
import com.kylecorry.trail_sense.shared.generated.resources.end_time
import com.kylecorry.trail_sense.shared.generated.resources.noon
import com.kylecorry.trail_sense.shared.generated.resources.peak_time
import com.kylecorry.trail_sense.shared.generated.resources.start_time
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.time.toLocalDate
import com.kylecorry.trail_sense.shared.views.TrailSenseColors
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

abstract class BaseAstroListItemProducer : AstronomyListItemProducer {

    // Drawn images (the moon's phase, eclipses) are made at a fixed size that stays sharp in the list's icon chip
    protected val imageSize = IMAGE_SIZE_PX
    protected val secondaryColor = TrailSenseColors.current.onSurfaceVariant.toArgb()
    protected val formatter = FormatService.getInstance()
    protected val astronomyService = AstronomyService()
    protected val prefs = getAppService<UserPreferences>()
    protected val strings = getAppService<AppStrings>()

    private fun formatTime(time: Instant?): String {
        if (time == null) {
            return "--:--"
        }
        return formatter.formatTime(time, false)
    }


    private fun datapoint(
        value: CharSequence,
        label: CharSequence? = null
    ): AstronomyListItemData {
        return AstronomyListItemData(
            value = value.toString(),
            label = label?.toString()
        )
    }

    protected fun percent(label: String, percent: Float): CharSequence {
        return "$label (${formatter.formatPercentage(percent)})"
    }

    protected fun list(
        id: Long,
        title: CharSequence,
        subtitle: CharSequence? = null,
        icon: AstronomyListIcon? = null,
        data: List<AstronomyListItemData> = listOf(),
        onClick: (() -> Unit)? = null
    ): AstronomyListItem {
        return AstronomyListItem(
            id = id,
            title = title.toString(),
            subtitle = subtitle?.toString(),
            icon = icon,
            data = data,
            onClick = onClick
        )
    }

    protected fun icon(vector: ImageVector, tint: Int? = null, rotation: Float = 0f): AstronomyListIcon {
        return AstronomyListIcon(vector, tint, rotation)
    }

    protected fun icon(bitmap: ImageBitmap): AstronomyListIcon {
        return AstronomyListIcon(bitmap = bitmap)
    }

    protected fun showAdvancedData(
        title: String,
        advancedData: List<Pair<CharSequence, List<AstronomyListItemData>?>>
    ) {
        val description = advancedData
            .filter { it.second != null }
            .joinToString("\n\n") { (section, data) ->
                val lines = data.orEmpty().joinToString("\n") {
                    if (it.label == null) {
                        it.value
                    } else {
                        "${it.label}: ${it.value}"
                    }
                }
                if (lines.isBlank()) {
                    section.toString()
                } else {
                    "${section}\n$lines"
                }
            }

        Alerts.dialog(
            AppHost.current(),
            title,
            description,
            cancelText = null
        )
    }

    // COMMON DATA POINT FIELDS
    protected fun data(value: CharSequence, label: CharSequence? = null): List<AstronomyListItemData> {
        return listOf(datapoint(value, label))
    }

    protected fun degrees(value: Float): List<AstronomyListItemData> {
        return data(formatter.formatDegrees(value))
    }

    protected fun direction(bearing: Bearing): List<AstronomyListItemData> {
        return data(formatter.formatDirection(bearing.direction))
    }

    protected fun decimal(value: Float, places: Int): List<AstronomyListItemData> {
        return data(DecimalFormatter.format(value, places))
    }

    protected fun percent(value: Float): List<AstronomyListItemData> {
        return data(formatter.formatPercentage(value))
    }

    protected fun duration(value: Duration): List<AstronomyListItemData> {
        return data(formatter.formatDuration(value, false))
    }

    protected fun riseSetTransit(times: RiseSetTransitTimes): List<AstronomyListItemData> {
        return listOf(
            strings.get(Res.string.astronomy_rise) to times.rise,
            strings.get(Res.string.noon) to times.transit,
            strings.get(Res.string.astronomy_set) to times.set
        ).sortedBy { it.second }.map {
            datapoint(formatTime(it.second), it.first)
        }
    }

    protected fun times(
        start: Instant?,
        peak: Instant?,
        end: Instant?,
        displayDate: LocalDate? = start?.toLocalDate()
    ): List<AstronomyListItemData> {
        val reserveHeaderSpace = listOf(start, peak, end).any { it != null && it.toLocalDate() != displayDate }
        return listOf(
            strings.get(Res.string.start_time) to start,
            strings.get(Res.string.peak_time) to peak,
            strings.get(Res.string.end_time) to end
        ).flatMap {
            time(it.second, displayDate, it.first, reserveHeaderSpace)
        }
    }

    protected fun time(
        time: Instant?,
        displayDate: LocalDate? = time?.toLocalDate(),
        label: CharSequence? = null,
        reserveHeaderSpace: Boolean = false
    ): List<AstronomyListItemData> {

        val value = buildString {
            if (time != null && time.toLocalDate() != displayDate) {
                appendLine(formatter.formatRelativeDate(time.toLocalDate(), true))
            } else if (reserveHeaderSpace) {
                appendLine()
            }
            append(formatTime(time))
        }
        return listOf(datapoint(value, label))
    }

    private companion object {
        const val IMAGE_SIZE_PX = 112
    }
}
