package com.kylecorry.trail_sense.shared.dialogs

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.time.nowLocal
import com.kylecorry.trail_sense.shared.time.today
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/*
 * The dialogs opened from Android code with a Context: each finds the AppHost of the activity behind the context
 * (or the running one) and opens the common dialog there.
 */

fun showComposeDialog(
    context: Context,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    onCancel: () -> Unit = {},
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
): TrailSenseDialog = showComposeDialog(AppHost.from(context), cancelable, cancelOnOutsideTouch, onCancel, content)

fun Alerts.dialog(
    context: Context,
    title: CharSequence,
    content: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    allowLinks: Boolean = false,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    scrollable: Boolean = false,
    body: (@Composable () -> Unit)? = null,
    onClose: ((cancelled: Boolean) -> Unit)? = null
): TrailSenseDialog = dialog(AppHost.from(context), title, content, okText, cancelText, allowLinks, cancelable, cancelOnOutsideTouch, scrollable, body, onClose)

fun Alerts.snackbar(
    context: Context,
    text: CharSequence,
    short: Boolean = true
) = snackbar(AppHost.from(context), text, short)

fun Alerts.loading(
    context: Context,
    title: CharSequence
): TrailSenseDialog = loading(AppHost.from(context), title)

fun Alerts.progress(
    context: Context,
    title: CharSequence
): Pair<TrailSenseDialog, (Float) -> Unit> = progress(AppHost.from(context), title)

inline fun <T> Alerts.withLoading(
    context: Context,
    title: CharSequence,
    action: () -> T
): T = withLoading(AppHost.from(context), title, action)

inline fun <T> Alerts.withProgress(
    context: Context,
    title: CharSequence,
    action: ((Float) -> Unit) -> T
): T = withProgress(AppHost.from(context), title, action)

suspend fun CoroutineAlerts.dialog(
    context: Context,
    title: CharSequence,
    content: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    allowLinks: Boolean = false,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    scrollable: Boolean = false,
    body: (@Composable () -> Unit)? = null
): Boolean = dialog(AppHost.from(context), title, content, okText, cancelText, allowLinks, cancelable, cancelOnOutsideTouch, scrollable, body)

fun Pickers.item(
    context: Context,
    title: CharSequence,
    items: List<CharSequence>,
    defaultSelectedIndex: Int = -1,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    onPick: (selectedIndex: Int?) -> Unit
) = item(AppHost.from(context), title, items, defaultSelectedIndex, okText, cancelText, onPick)

fun Pickers.items(
    context: Context,
    title: CharSequence,
    items: List<CharSequence>,
    defaultSelectedIndices: List<Int> = emptyList(),
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    onPick: (selectedIndices: List<Int>?) -> Unit
) = items(AppHost.from(context), title, items, defaultSelectedIndices, okText, cancelText, onPick)

fun Pickers.text(
    context: Context,
    title: CharSequence,
    description: CharSequence? = null,
    default: String? = null,
    hint: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    onTextPick: (text: String?) -> Unit
) = text(AppHost.from(context), title, description, default, hint, okText, cancelText, onTextPick)

fun Pickers.number(
    context: Context,
    title: CharSequence,
    description: CharSequence? = null,
    default: Number? = null,
    allowDecimals: Boolean = true,
    allowNegative: Boolean = false,
    hint: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    onNumberPick: (number: Number?) -> Unit
) = number(AppHost.from(context), title, description, default, allowDecimals, allowNegative, hint, okText, cancelText, onNumberPick)

fun Pickers.time(
    context: Context,
    use24Hours: Boolean,
    default: LocalTime = nowLocal().time,
    onTimePick: (time: LocalTime?) -> Unit
) = time(AppHost.from(context), use24Hours, default, onTimePick)

fun Pickers.date(
    context: Context,
    default: LocalDate = today(),
    onDatePick: (date: LocalDate?) -> Unit
) = date(AppHost.from(context), default, onDatePick)

fun Pickers.datetime(
    context: Context,
    use24Hours: Boolean,
    default: LocalDateTime = nowLocal(),
    onDatetimePick: (value: LocalDateTime?) -> Unit
) = datetime(AppHost.from(context), use24Hours, default, onDatetimePick)

fun Pickers.menu(
    context: Context,
    items: List<CharSequence?>,
    onSelection: (index: Int) -> Boolean
) = menu(AppHost.from(context), items, onSelection)

suspend fun CoroutinePickers.text(
    context: Context,
    title: CharSequence,
    description: CharSequence? = null,
    default: String? = null,
    hint: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel
): String? = text(AppHost.from(context), title, description, default, hint, okText, cancelText)

suspend fun CoroutinePickers.number(
    context: Context,
    title: CharSequence,
    description: CharSequence? = null,
    default: Number? = null,
    allowDecimals: Boolean = true,
    allowNegative: Boolean = false,
    hint: CharSequence? = null,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel
): Number? = number(AppHost.from(context), title, description, default, allowDecimals, allowNegative, hint, okText, cancelText)

suspend fun CoroutinePickers.item(
    context: Context,
    title: CharSequence,
    items: List<CharSequence>,
    defaultSelectedIndex: Int = -1,
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel
): Int? = item(AppHost.from(context), title, items, defaultSelectedIndex, okText, cancelText)

suspend fun CoroutinePickers.items(
    context: Context,
    title: CharSequence,
    items: List<CharSequence>,
    defaultSelectedIndices: List<Int> = emptyList(),
    okText: CharSequence? = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel
): List<Int>? = items(AppHost.from(context), title, items, defaultSelectedIndices, okText, cancelText)

suspend fun CoroutinePickers.time(
    context: Context,
    use24Hours: Boolean,
    default: LocalTime = nowLocal().time
): LocalTime? = time(AppHost.from(context), use24Hours, default)

suspend fun CoroutinePickers.date(
    context: Context,
    default: LocalDate = today()
): LocalDate? = date(AppHost.from(context), default)

suspend fun CoroutinePickers.datetime(
    context: Context,
    use24Hours: Boolean,
    default: LocalDateTime = nowLocal()
): LocalDateTime? = datetime(AppHost.from(context), use24Hours, default)
