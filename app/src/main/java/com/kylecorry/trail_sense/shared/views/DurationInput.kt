package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import java.time.Duration
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField

@Composable
fun DurationPickerDialog(
    title: String,
    duration: Duration?,
    onDurationPicked: (Duration?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    hint: String = stringResource(R.string.duration),
    showSeconds: Boolean = false
) {
    var selectedDuration by remember(duration) { mutableStateOf(duration) }

    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onDurationPicked(selectedDuration) },
        dismissText = stringResource(android.R.string.cancel)
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            DurationInput(
                duration = duration,
                onDurationChange = {
                    selectedDuration = it
                },
                hint = hint,
                showSeconds = showSeconds,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DurationInput(
    duration: Duration?,
    onDurationChange: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.duration),
    showSeconds: Boolean = true,
    allowNegative: Boolean = false
) {
    val context = LocalContext.current
    var durationText by remember(duration, showSeconds) {
        mutableStateOf(duration.toDurationText(showSeconds))
    }
    var isNegative by remember(duration, allowNegative) {
        mutableStateOf(allowNegative && duration?.isNegative == true)
    }
    var fieldValue by remember(durationText, isNegative, showSeconds) {
        val displayText = durationText.toDisplayDuration(context, showSeconds, isNegative)
        mutableStateOf(TextFieldValue(displayText, TextRange(displayText.length)))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (allowNegative) {
            IconButton(
                onClick = {
                    isNegative = !isNegative
                    val displayText = durationText.toDisplayDuration(context, showSeconds, isNegative)
                    fieldValue = TextFieldValue(displayText, TextRange(displayText.length))
                    onDurationChange(durationText.toDuration(isNegative))
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (isNegative) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null
                )
            }
        }

        TrailSenseInputField(
            value = fieldValue,
            onValueChange = { newValue ->
                val oldDigits = fieldValue.text.filter(Char::isDigit)
                val newDigits = newValue.text.filter(Char::isDigit)
                durationText = when {
                    newDigits.length < oldDigits.length -> {
                        durationText.removeDigit(if (showSeconds) PLACES_SECONDS else PLACES_MINUTES)
                    }

                    newDigits.length > oldDigits.length -> {
                        val digit = newDigits.last().digitToIntOrNull()
                        if (digit != null) {
                            durationText.appendDigit(
                                digit,
                                if (showSeconds) PLACES_SECONDS else PLACES_MINUTES
                            )
                        } else {
                            durationText
                        }
                    }

                    else -> durationText
                }
                val displayText = durationText.toDisplayDuration(context, showSeconds, isNegative)
                fieldValue = TextFieldValue(displayText, TextRange(displayText.length))
                onDurationChange(durationText.toDuration(isNegative))
            },
            modifier = Modifier.fillMaxWidth(),
            label = hint,
            keyboardType = KeyboardType.Number,
            trailingIcon = {
                IconButton(
                    onClick = {
                        durationText = EMPTY_DURATION_TEXT
                        isNegative = false
                        val displayText = durationText.toDisplayDuration(context, showSeconds, false)
                        fieldValue = TextFieldValue(displayText, TextRange(displayText.length))
                        onDurationChange(durationText.toDuration(false))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

private fun Duration?.toDurationText(showSeconds: Boolean): String {
    val clamped = (this?.abs() ?: Duration.ZERO).coerceAtMost(MAX_DURATION)
    val hours = clamped.toHours().toInt()
    val minutes = clamped.toMinutes().toInt() % 60
    val seconds = if (showSeconds) clamped.seconds.toInt() % 60 else 0
    return "%02d%02d%02d".format(hours, minutes, seconds)
}

private fun String.removeDigit(place: Int): String {
    return ("0" + substring(0, place)).padEnd(6, '0')
}

private fun String.appendDigit(digit: Int, place: Int): String {
    if (this[0] != '0') {
        return this
    }
    return (substring(1, place + 1) + digit.toString()).padEnd(6, '0')
}

private fun String.toDuration(isNegative: Boolean): Duration {
    val hours = substring(0, 2).toInt()
    val minutes = substring(2, 4).toInt()
    val seconds = substring(4, 6).toInt()
    val duration = Duration.ofHours(hours.toLong())
        .plusMinutes(minutes.toLong())
        .plusSeconds(seconds.toLong())
    return if (isNegative) {
        duration.negated()
    } else {
        duration
    }
}

private fun String.toDisplayDuration(
    context: android.content.Context,
    showSeconds: Boolean,
    isNegative: Boolean
): String {
    val hours = substring(0, 2)
    val minutes = substring(2, 4)
    val seconds = substring(4, 6)
    val hoursSymbol = context.getString(R.string.hours_format, "").trim()
    val minutesSymbol = context.getString(R.string.minutes_format, "").trim()
    val secondsSymbol = context.getString(R.string.seconds_format, "").trim()
    val prefix = if (isNegative) "- " else ""
    return buildString {
        append(prefix)
        append(hours)
        append(hoursSymbol)
        append(" ")
        append(minutes)
        append(minutesSymbol)
        if (showSeconds) {
            append(" ")
            append(seconds)
            append(secondsSymbol)
        }
    }
}

private val MAX_DURATION = Duration.ofHours(99).plusMinutes(99).plusSeconds(99)
private const val EMPTY_DURATION_TEXT = "000000"
private const val PLACES_MINUTES = 3
private const val PLACES_SECONDS = 5
