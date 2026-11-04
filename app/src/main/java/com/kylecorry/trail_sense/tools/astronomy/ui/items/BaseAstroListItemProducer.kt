package com.kylecorry.trail_sense.tools.astronomy.ui.items

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import android.graphics.Bitmap
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class BaseAstroListItemProducer(protected val context: Context) :
    AstronomyListItemProducer {

    protected val imageSize = Resources.dp(context, 24f).toInt()
    protected val secondaryColor = Resources.androidTextColorSecondary(context)
    protected val formatter = FormatService.getInstance(context)
    protected val astronomyService = AstronomyService()
    protected val prefs = UserPreferences(context)

    private fun formatTime(time: ZonedDateTime?): String {
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

    protected fun icon(bitmap: Bitmap): AstronomyListIcon {
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
            context,
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
            context.getString(R.string.astronomy_rise) to times.rise,
            context.getString(R.string.noon) to times.transit,
            context.getString(R.string.astronomy_set) to times.set
        ).sortedBy { it.second }.map {
            datapoint(formatTime(it.second), it.first)
        }
    }

    protected fun times(
        start: ZonedDateTime?,
        peak: ZonedDateTime?,
        end: ZonedDateTime?,
        displayDate: LocalDate? = start?.toLocalDate()
    ): List<AstronomyListItemData> {
        val reserveHeaderSpace = listOf(start, peak, end).any { it != null && it.toLocalDate() != displayDate }
        return listOf(
            context.getString(R.string.start_time) to start,
            context.getString(R.string.peak_time) to peak,
            context.getString(R.string.end_time) to end
        ).flatMap {
            time(it.second, displayDate, it.first, reserveHeaderSpace)
        }
    }

    protected fun time(
        time: ZonedDateTime?,
        displayDate: LocalDate? = time?.toLocalDate(),
        label: CharSequence? = null,
        reserveHeaderSpace: Boolean = false
    ): List<AstronomyListItemData> {

        val value = buildSpannedString {
            scale(0.75f) {
                if (time != null && time.toLocalDate() != displayDate) {
                    appendLine(formatter.formatRelativeDate(time.toLocalDate(), true))
                } else if (reserveHeaderSpace) {
                    appendLine()
                }
            }
            append(formatTime(time))
        }
        return listOf(datapoint(value, label))
    }


}
