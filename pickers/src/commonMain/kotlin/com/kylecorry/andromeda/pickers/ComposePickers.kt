package com.kylecorry.andromeda.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.forms.FormNumberField
import com.kylecorry.andromeda.forms.FormTextField
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initial: LocalDate, onResult: (LocalDate?) -> Unit,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel",
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
) {
    val initialMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis, selectableDates = selectableDates)
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = { onResult(null) },
        confirmButton = { TextButton(onClick = {
            val date = state.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
            onResult(date)
        }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } }
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: LocalTime, onResult: (LocalTime?) -> Unit, use24Hours: Boolean = true,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = use24Hours)
    AlertDialog(onDismissRequest = { onResult(null) },
        confirmButton = { TextButton(onClick = { onResult(LocalTime(state.hour, state.minute)) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } },
        text = { TimePicker(state) })
}

@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime, onResult: (LocalDateTime?) -> Unit, use24Hours: Boolean = true,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    var date by remember { mutableStateOf<LocalDate?>(null) }
    if (date == null) {
        DatePickerDialog(initial.date, { selected -> if (selected == null) onResult(null) else date = selected }, confirmLabel, cancelLabel)
    } else {
        TimePickerDialog(initial.time, { time -> onResult(time?.let { LocalDateTime(date!!, it) }) }, use24Hours, confirmLabel, cancelLabel)
    }
}

@Composable
fun <T> ItemPickerDialog(
    title: String, items: List<T>, selected: T?, label: (T) -> String, onResult: (T?) -> Unit,
    cancelLabel: String = "Cancel"
) {
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) },
        text = { LazyColumn { itemsIndexed(items) { _, item ->
            Row(Modifier.fillMaxWidth().clickable { onResult(item) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = item == selected, onClick = null)
                Text(label(item), Modifier.padding(start = 8.dp))
            }
        } } }, confirmButton = {}, dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } })
}

@Composable
fun <T> MultiItemPickerDialog(
    title: String, items: List<T>, selected: Set<T>, label: (T) -> String, onResult: (Set<T>?) -> Unit,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    val chosen = remember { mutableStateMapOf<T, Boolean>().apply { items.forEach { put(it, it in selected) } } }
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) },
        text = { LazyColumn { itemsIndexed(items) { _, item ->
            val checked = chosen[item] == true
            Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = { chosen[item] = it }).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onCheckedChange = null)
                Text(label(item), Modifier.padding(start = 8.dp))
            }
        } } },
        confirmButton = { TextButton(onClick = { onResult(chosen.filterValues { it }.keys.toSet()) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } })
}

@Composable
fun TextPickerDialog(
    title: String, initial: String, label: String, onResult: (String?) -> Unit,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) }, text = { FormTextField(value, { value = it }, label) },
        confirmButton = { TextButton(onClick = { onResult(value) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } })
}

@Composable
fun NumberPickerDialog(
    title: String, initial: String, label: String, onResult: (Double?) -> Unit,
    allowDecimals: Boolean = true, allowNegative: Boolean = false, confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) },
        text = { FormNumberField(value, { value = it }, label, allowDecimals = allowDecimals, allowNegative = allowNegative) },
        confirmButton = { TextButton(onClick = { onResult(value.replace(',', '.').toDoubleOrNull()) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } })
}

@Composable
fun PickerMenu(items: List<MenuItem>, modifier: Modifier = Modifier, trigger: @Composable (onClick: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        trigger { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item.name.toString()) }, onClick = { if (item.action()) expanded = false })
            }
        }
    }
}

/** Calendar with a composable day slot. Weekday labels are ordered Monday through Sunday. */
@Composable
fun DecoratedDatePickerDialog(
    initial: LocalDate,
    onResult: (LocalDate?) -> Unit,
    monthLabel: (LocalDate) -> String,
    weekdayLabels: List<String>,
    confirmLabel: String = "OK",
    cancelLabel: String = "Cancel",
    previousMonthLabel: String = "Previous month",
    nextMonthLabel: String = "Next month",
    isDateEnabled: (LocalDate) -> Boolean = { true },
    dayContent: @Composable (LocalDate, Boolean) -> Unit = { date, _ -> Text(date.day.toString()) }
) {
    require(weekdayLabels.size == 7)
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(LocalDate(initial.year, initial.month, 1)) }
    val firstEpoch = month.toEpochDays()
    val nextMonth = if (month.month.ordinal == 11) LocalDate(month.year + 1, 1, 1) else LocalDate(month.year, month.month.ordinal + 2, 1)
    val previousMonth = if (month.month.ordinal == 0) LocalDate(month.year - 1, 12, 1) else LocalDate(month.year, month.month.ordinal, 1)
    val offset = month.dayOfWeek.ordinal
    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(monthLabel(month)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { month = previousMonth }) { Text(previousMonthLabel) }
                    TextButton(onClick = { month = nextMonth }) { Text(nextMonthLabel) }
                }
                Row(Modifier.fillMaxWidth()) {
                    weekdayLabels.forEach { label -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label) } }
                }
                repeat(6) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { day ->
                            val date = LocalDate.fromEpochDays(firstEpoch - offset + week * 7 + day)
                            val inMonth = date.month == month.month
                            val enabled = inMonth && isDateEnabled(date)
                            val selectedDay = date == selected
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (inMonth) {
                                    Surface(
                                        color = if (selectedDay) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                        contentColor = if (selectedDay) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxSize().clickable(enabled = enabled) { selected = date }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { dayContent(date, selectedDay) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onResult(selected) }, enabled = isDateEnabled(selected)) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } }
    )
}

@Composable
fun MultiIndexPickerDialog(
    title: String, items: List<String>, selected: Set<Int>, onResult: (Set<Int>?) -> Unit,
    confirmLabel: String = "OK", cancelLabel: String = "Cancel"
) {
    var chosen by remember { mutableStateOf(selected) }
    AlertDialog(onDismissRequest = { onResult(null) }, title = { Text(title) },
        text = { LazyColumn { itemsIndexed(items) { index, item ->
            val checked = index in chosen
            Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = {
                chosen = if (it) chosen + index else chosen - index
            }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onCheckedChange = null)
                Text(item, Modifier.padding(start = 8.dp))
            }
        } } },
        confirmButton = { TextButton(onClick = { onResult(chosen) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { onResult(null) }) { Text(cancelLabel) } })
}
