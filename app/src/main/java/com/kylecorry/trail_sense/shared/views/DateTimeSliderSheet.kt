package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider as ComposeSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

class DateTimeSliderSheetState(context: Context) {

    private val formatter = getAppService<FormatService>()
    private val hooks = Hooks(
        stateThrottleMs = 300,
        stateTriggerOnStart = false,
        stateOnChange = {
            onTimeChanged?.invoke(currentTime.toInstant())
        }
    )
    private var trigger by hooks.state("")
    private var currentTime by mutableStateOf(ZonedDateTime.now())
    private var sheetVisible by mutableStateOf(false)

    var onTimeChanged: ((Instant?) -> Unit)? = null

    var isVisible: Boolean
        get() = sheetVisible
        set(value) {
            sheetVisible = value
        }

    fun setTime(instant: Instant?) {
        currentTime = instant?.toZonedDateTime() ?: ZonedDateTime.now()
    }

    private fun notifyChanged() {
        trigger = UUID.randomUUID().toString()
    }

    fun show() {
        hooks.startStateUpdates()
        isVisible = true
    }

    fun hide() {
        hooks.stopStateUpdates()
        isVisible = false
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!sheetVisible) {
            return
        }
        DateTimeSliderSheetContent(
            currentTime = currentTime,
            formatter = formatter,
            onDateChanged = {
                currentTime = currentTime.with(it)
                notifyChanged()
            },
            onTimeChanged = {
                currentTime = currentTime.with(it)
                notifyChanged()
            },
            modifier = modifier
        )
    }
}

@Composable
private fun DateTimeSliderSheetContent(
    currentTime: ZonedDateTime,
    formatter: FormatService,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val minutes = (currentTime.hour * 60 + currentTime.minute).toFloat()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DatePickerRow(
                date = currentTime.toLocalDate(),
                formatter = formatter,
                onDateChanged = onDateChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_picker")
            )
            Text(
                text = formatter.formatTime(currentTime.toLocalTime(), includeSeconds = false),
                modifier = Modifier
                    .padding(top = TrailSenseSpacing.lg)
                    .heightIn(min = 48.dp)
                    .clickable {
                        CustomUiUtils.pickTime(
                            context,
                            prefs.use24HourTime,
                            currentTime.toLocalTime()
                        ) {
                            if (it != null) {
                                onTimeChanged(it)
                            }
                        }
                    }
                    .testTag("time_text"),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            ComposeSlider(
                value = minutes.coerceIn(0f, Duration.ofDays(1).toMinutes().toFloat()),
                onValueChange = {
                    val seconds = (it.toInt() * 60L)
                        .coerceAtMost(LocalTime.MAX.toSecondOfDay().toLong())
                    onTimeChanged(LocalTime.ofSecondOfDay(seconds))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_slider"),
                valueRange = 0f..Duration.ofDays(1).toMinutes().toFloat()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatePickerRow(
    date: LocalDate,
    formatter: FormatService,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDateChanged(date.minusDays(1)) },
            modifier = Modifier
                .size(48.dp)
                .testTag("prev_date")
        ) {
            Icon(
                imageVector = TrailIcons.KeyboardArrowRight,
                contentDescription = stringResource(R.string.previous),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = formatter.formatRelativeDate(date),
            modifier = Modifier.testTag("date"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .padding(start = TrailSenseSpacing.lg)
                .size(48.dp)
                .combinedClickable(
                    onClick = {
                        CustomUiUtils.pickDate(context, date) {
                            if (it != null) {
                                onDateChanged(it)
                            }
                        }
                    },
                    onLongClick = { onDateChanged(LocalDate.now()) }
                )
                .testTag("date_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TrailIcons.Date,
                contentDescription = stringResource(R.string.date),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { onDateChanged(date.plusDays(1)) },
            modifier = Modifier
                .size(48.dp)
                .testTag("next_date")
        ) {
            Icon(
                imageVector = TrailIcons.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
