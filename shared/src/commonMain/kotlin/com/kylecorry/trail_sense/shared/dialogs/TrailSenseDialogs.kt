@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kylecorry.trail_sense.shared.dialogs

import com.kylecorry.trail_sense.shared.time.nowLocal

import com.kylecorry.trail_sense.shared.time.today

import com.kylecorry.trail_sense.shared.time.toLocalDate

import com.kylecorry.trail_sense.shared.time.atStartOfDay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.toAnnotatedString
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
/*
 * Trail Sense's dialogs, drawn in Compose so they look like the rest of the app (28 dp corners, a headline title, the
 * app's colors and type). They keep Andromeda's names and parameters, so callers open them imperatively from any
 * code with the [AppHost] to draw them in; each dialog is a Compose Dialog that host draws. (On Android, overloads
 * taking a Context find the activity's host.)
 */

/**
 * Shows [content] as a dialog and returns it, so callers can still dismiss it. [onCancel] runs for back, a tap
 * outside (when allowed), or the cancel button's own handling.
 */
fun showComposeDialog(
    host: AppHost,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    onCancel: () -> Unit = {},
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
): TrailSenseDialog {
    return host.dialogs.show(onCancel = onCancel) { dialog ->
        Dialog(
            onDismissRequest = { dialog.cancel() },
            properties = DialogProperties(
                dismissOnBackPress = cancelable,
                dismissOnClickOutside = cancelable && cancelOnOutsideTouch,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .imePadding()
                    .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(modifier = Modifier.padding(top = TrailSenseSpacing.xl)) {
                        content { dialog.dismiss() }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(title: CharSequence) {
    if (title.isEmpty()) return
    Text(
        text = title.toString(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.xl)
            .padding(bottom = TrailSenseSpacing.lg)
    )
}

/** The message in the body type and color; styled text (markdown, HTML) keeps its styles, and links open. */
@Composable
private fun DialogMessage(text: CharSequence, allowLinks: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.bodyMedium
    val linkColor = MaterialTheme.colorScheme.primary
    if (text is AnnotatedString || allowLinks) {
        val annotated = remember(text, linkColor, allowLinks) {
            text.toAnnotatedString(linkColor, linksClickable = allowLinks)
        }
        // The old TextView added 15% to its natural line height (about 1.17 times the text size).
        Text(
            text = annotated,
            style = style.copy(lineHeight = style.fontSize * 1.17f * 1.15f),
            color = color,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Text(text = text.toString(), style = style, color = color, modifier = modifier.fillMaxWidth())
    }
}

/** The buttons, end-aligned: cancel then confirm, both text buttons in the app's primary color. */
@Composable
private fun DialogButtons(
    okText: CharSequence?,
    cancelText: CharSequence?,
    okEnabled: Boolean = true,
    onOk: () -> Unit,
    onCancel: () -> Unit
) {
    if (okText == null && cancelText == null) {
        Spacer(Modifier.size(TrailSenseSpacing.xl))
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.lg, top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm, Alignment.End)
    ) {
        if (cancelText != null) {
            TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) { Text(cancelText.label()) }
        }
        if (okText != null) {
            Button(onClick = onOk, enabled = okEnabled, shapes = ButtonDefaults.shapes()) {
                Text(okText.label())
            }
        }
    }
}

object Alerts {

    /**
     * A dialog with a [title], an optional [content] message, and an optional [body] drawn below it.
     */
    fun dialog(
        host: AppHost,
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
    ): TrailSenseDialog {
        var closed = false
        fun close(cancelled: Boolean) {
            if (closed) return
            closed = true
            onClose?.invoke(cancelled)
        }
        val hasBody = body != null
        return showComposeDialog(
            host,
            cancelable = cancelable,
            cancelOnOutsideTouch = cancelOnOutsideTouch,
            onCancel = { close(true) }
        ) { dismiss ->
            DialogTitle(title)
            // Text scrolls on its own; a custom body keeps its own scrolling unless the caller asked for it.
            val scroll = if (!hasBody || scrollable) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(scroll)
                    .padding(horizontal = TrailSenseSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                if (content != null) DialogMessage(content, allowLinks)
                if (body != null) Box(Modifier.fillMaxWidth()) { body() }
            }
            DialogButtons(
                okText = okText,
                cancelText = cancelText,
                onOk = {
                    dismiss()
                    close(false)
                },
                onCancel = {
                    dismiss()
                    close(true)
                }
            )
        }
    }

    /**
     * A short message that needs no answer, as a snackbar in the app-wide [AppHost] (a Compose SnackbarHostState).
     * Asked for while the app is in the background, it waits and shows once the app is on screen again.
     */
    fun snackbar(host: AppHost, text: CharSequence, short: Boolean = true) {
        host.snackbar(text.toString(), long = !short)
    }

    /** A dialog with [title] and a spinner that can't be dismissed by the person; dismiss it when the work is done. */
    fun loading(host: AppHost, title: CharSequence): TrailSenseDialog {
        return showComposeDialog(host, cancelable = false, cancelOnOutsideTouch = false) {
            DialogTitle(title)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = TrailSenseSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    /** A dialog with [title] and a progress bar; the returned function sets the progress (0 to 1) from any thread. */
    fun progress(host: AppHost, title: CharSequence): Pair<TrailSenseDialog, (Float) -> Unit> {
        val progress = mutableFloatStateOf(0f)
        val dialog = showComposeDialog(host, cancelable = false, cancelOnOutsideTouch = false) {
            DialogTitle(title)
            ProgressBody(progress = { progress.floatValue })
            Spacer(Modifier.size(TrailSenseSpacing.xl))
        }
        return dialog to { value -> onMain { progress.floatValue = value.coerceIn(0f, 1f) } }
    }

    inline fun <T> withLoading(host: AppHost, title: CharSequence, action: () -> T): T {
        val loading = loading(host, title)
        try {
            return action()
        } finally {
            loading.dismiss()
        }
    }

    inline fun <T> withProgress(host: AppHost, title: CharSequence, action: ((Float) -> Unit) -> T): T {
        val (dialog, setProgress) = progress(host, title)
        try {
            return action(setProgress)
        } finally {
            dialog.dismiss()
        }
    }
}

/** A full-width determinate progress bar, inset like the dialog's text. */
@Composable
fun ProgressBody(progress: () -> Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.sm)
    )
}

object CoroutineAlerts {
    /** Returns true if cancelled. */
    suspend fun dialog(
        host: AppHost,
        title: CharSequence,
        content: CharSequence? = null,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel,
        allowLinks: Boolean = false,
        cancelable: Boolean = true,
        cancelOnOutsideTouch: Boolean = true,
        scrollable: Boolean = false,
        body: (@Composable () -> Unit)? = null
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val dialog = Alerts.dialog(
            host, title, content, okText, cancelText, allowLinks, cancelable,
            cancelOnOutsideTouch, scrollable, body
        ) { cancelled ->
            if (continuation.isActive) continuation.resume(cancelled)
        }
        continuation.invokeOnCancellation { dialog.dismiss() }
    }
}

object Pickers {

    /** One choice: tapping a row picks it and closes, like Android's own lists. */
    fun item(
        host: AppHost,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndex: Int = -1,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel,
        onPick: (selectedIndex: Int?) -> Unit
    ) {
        var done = false
        fun pick(index: Int?) {
            if (done) return
            done = true
            onPick(index)
        }
        showComposeDialog(host, onCancel = { pick(null) }) { dismiss ->
            DialogTitle(title)
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (defaultSelectedIndex - 2).coerceAtLeast(0))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .selectableGroup()
            ) {
                itemsIndexed(items) { index, item ->
                    ChoiceRow(
                        label = item,
                        selected = index == defaultSelectedIndex,
                        multiple = false,
                        onClick = {
                            dismiss()
                            pick(index)
                        }
                    )
                }
            }
            DialogButtons(okText = null, cancelText = cancelText, onOk = {}, onCancel = {
                dismiss()
                pick(null)
            })
        }
    }

    /** Several choices, confirmed with OK. */
    fun items(
        host: AppHost,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndices: List<Int> = emptyList(),
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel,
        onPick: (selectedIndices: List<Int>?) -> Unit
    ) {
        var done = false
        fun pick(indices: List<Int>?) {
            if (done) return
            done = true
            onPick(indices)
        }
        showComposeDialog(host, onCancel = { pick(null) }) { dismiss ->
            var selected by remember { mutableStateOf(defaultSelectedIndices.toSet()) }
            DialogTitle(title)
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(items) { index, item ->
                    ChoiceRow(
                        label = item,
                        selected = index in selected,
                        multiple = true,
                        onClick = { selected = if (index in selected) selected - index else selected + index }
                    )
                }
            }
            DialogButtons(
                okText = okText,
                cancelText = cancelText,
                onOk = {
                    dismiss()
                    pick(selected.sorted())
                },
                onCancel = {
                    dismiss()
                    pick(null)
                }
            )
        }
    }

    fun text(
        host: AppHost,
        title: CharSequence,
        description: CharSequence? = null,
        default: String? = null,
        hint: CharSequence? = null,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel,
        onTextPick: (text: String?) -> Unit
    ) {
        inputDialog(host, title, description, default, hint, KeyboardType.Text, okText, cancelText) {
            onTextPick(it)
        }
    }

    fun number(
        host: AppHost,
        title: CharSequence,
        description: CharSequence? = null,
        default: Number? = null,
        allowDecimals: Boolean = true,
        allowNegative: Boolean = false,
        hint: CharSequence? = null,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel,
        onNumberPick: (number: Number?) -> Unit
    ) {
        val start = default?.let { if (allowDecimals) it.toString() else it.toLong().toString() }
        inputDialog(
            host, title, description, start, hint,
            if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number, okText, cancelText,
            accept = { text ->
                val value = text.toDoubleCompat()
                value != null && (allowNegative || value >= 0) && (allowDecimals || value % 1.0 == 0.0)
            }
        ) { text ->
            val value = text?.toDoubleCompat()
            onNumberPick(
                when {
                    value == null -> null
                    allowDecimals -> value
                    else -> value.toLong().toInt()
                }
            )
        }
    }

    private fun inputDialog(
        host: AppHost,
        title: CharSequence,
        description: CharSequence?,
        default: String?,
        hint: CharSequence?,
        keyboard: KeyboardType,
        okText: CharSequence?,
        cancelText: CharSequence?,
        accept: (String) -> Boolean = { true },
        onPick: (String?) -> Unit
    ) {
        var done = false
        fun pick(text: String?) {
            if (done) return
            done = true
            onPick(text)
        }
        showComposeDialog(host, cancelOnOutsideTouch = false, onCancel = { pick(null) }) { dismiss ->
            val initial = default ?: ""
            var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
            val focus = remember { FocusRequester() }
            val valid = value.text.isEmpty() && keyboard == KeyboardType.Text || accept(value.text)
            fun confirm() {
                if (!valid) return
                dismiss()
                pick(value.text)
            }
            DialogTitle(title)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                if (description != null) DialogMessage(description, allowLinks = false)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    label = hint?.let { { Text(it.toString()) } },
                    singleLine = keyboard != KeyboardType.Text,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() })
                )
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
            DialogButtons(
                okText = okText,
                cancelText = cancelText,
                okEnabled = valid,
                onOk = { confirm() },
                onCancel = {
                    dismiss()
                    pick(null)
                }
            )
        }
    }

    /** The Material time picker (a dial), in 24-hour time when [use24Hours]. */
    fun time(
        host: AppHost,
        use24Hours: Boolean,
        default: LocalTime = nowLocal().time,
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        var done = false
        fun pick(time: LocalTime?) {
            if (done) return
            done = true
            onTimePick(time)
        }
        showComposeDialog(host, onCancel = { pick(null) }) { dismiss ->
            val state = rememberTimePickerState(default.hour, default.minute, use24Hours)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
            DialogButtons(
                okText = DialogLabels.Ok,
                cancelText = DialogLabels.Cancel,
                onOk = {
                    dismiss()
                    pick(LocalTime(state.hour, state.minute))
                },
                onCancel = {
                    dismiss()
                    pick(null)
                }
            )
        }
    }

    /** The Material date picker (a calendar). */
    fun date(
        host: AppHost,
        default: LocalDate = today(),
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        var done = false
        fun pick(date: LocalDate?) {
            if (done) return
            done = true
            onDatePick(date)
        }
        host.dialogs.show(onCancel = { pick(null) }) { dialog ->
            val state = rememberDatePickerState(
                initialSelectedDateMillis = default.atStartOfDay(TimeZone.UTC).toEpochMilliseconds()
            )
            DatePickerDialog(
                onDismissRequest = { dialog.cancel() },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.dismiss()
                            pick(state.selectedDateMillis?.let {
                                Instant.fromEpochMilliseconds(it).toLocalDate(TimeZone.UTC)
                            } ?: default)
                        },
                        shapes = ButtonDefaults.shapes()
                    ) { Text(systemString(SystemString.Ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { dialog.cancel() }, shapes = ButtonDefaults.shapes()) {
                        Text(systemString(SystemString.Cancel))
                    }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }

    fun datetime(
        host: AppHost,
        use24Hours: Boolean,
        default: LocalDateTime = nowLocal(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        date(host, default.date) { date ->
            if (date == null) {
                onDatetimePick(null)
                return@date
            }
            time(host, use24Hours, default.time) { time ->
                onDatetimePick(time?.let { LocalDateTime(date, it) })
            }
        }
    }

    /**
     * A menu of [items] (null items are left out) at the top end of the screen; [onSelection] gets the index of the
     * chosen item.
     */
    fun menu(host: AppHost, items: List<CharSequence?>, onSelection: (index: Int) -> Boolean) {
        host.dialogs.show { dialog ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                DropdownMenu(expanded = true, onDismissRequest = { dialog.dismiss() }) {
                    items.forEachIndexed { index, item ->
                        if (item != null) {
                            DropdownMenuItem(
                                text = { Text(item.toString()) },
                                onClick = {
                                    dialog.dismiss()
                                    onSelection(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: CharSequence, selected: Boolean, multiple: Boolean, onClick: () -> Unit) {
    val rowModifier = if (multiple) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(rowModifier)
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        if (multiple) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
        Text(
            text = label.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected && !multiple) FontWeight.SemiBold else null
        )
    }
}

object CoroutinePickers {
    suspend fun text(
        host: AppHost,
        title: CharSequence,
        description: CharSequence? = null,
        default: String? = null,
        hint: CharSequence? = null,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel
    ): String? = suspendCancellableCoroutine { c ->
        Pickers.text(host, title, description, default, hint, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun number(
        host: AppHost,
        title: CharSequence,
        description: CharSequence? = null,
        default: Number? = null,
        allowDecimals: Boolean = true,
        allowNegative: Boolean = false,
        hint: CharSequence? = null,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel
    ): Number? = suspendCancellableCoroutine { c ->
        Pickers.number(host, title, description, default, allowDecimals, allowNegative, hint, okText, cancelText) {
            if (c.isActive) c.resume(it)
        }
    }

    suspend fun item(
        host: AppHost,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndex: Int = -1,
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel
    ): Int? = suspendCancellableCoroutine { c ->
        Pickers.item(host, title, items, defaultSelectedIndex, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun items(
        host: AppHost,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndices: List<Int> = emptyList(),
        okText: CharSequence? = DialogLabels.Ok,
        cancelText: CharSequence? = DialogLabels.Cancel
    ): List<Int>? = suspendCancellableCoroutine { c ->
        Pickers.items(host, title, items, defaultSelectedIndices, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun time(host: AppHost, use24Hours: Boolean, default: LocalTime = nowLocal().time): LocalTime? =
        suspendCancellableCoroutine { c ->
            Pickers.time(host, use24Hours, default) { if (c.isActive) c.resume(it) }
        }

    suspend fun date(host: AppHost, default: LocalDate = today()): LocalDate? =
        suspendCancellableCoroutine { c ->
            Pickers.date(host, default) { if (c.isActive) c.resume(it) }
        }

    suspend fun datetime(
        host: AppHost,
        use24Hours: Boolean,
        default: LocalDateTime = nowLocal()
    ): LocalDateTime? = suspendCancellableCoroutine { c ->
        Pickers.datetime(host, use24Hours, default) { if (c.isActive) c.resume(it) }
    }
}

/**
 * Button labels the platform translates, used as the dialogs' default OK and Cancel text (pass null to hide a
 * button, or any text to replace it).
 */
object DialogLabels {
    val Ok: CharSequence = SystemLabel(SystemString.Ok)
    val Cancel: CharSequence = SystemLabel(SystemString.Cancel)
}

private class SystemLabel(val string: SystemString) : CharSequence by string.name

@Composable
private fun CharSequence.label(): String {
    return if (this is SystemLabel) systemString(string) else toString()
}
