@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kylecorry.trail_sense.shared.dialogs

import android.content.Context
import android.text.Spanned
import android.view.View
import android.util.Log
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.composeContentOf
import com.kylecorry.trail_sense.shared.views.compose.toAnnotatedString
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import com.kylecorry.andromeda.alerts.Alerts as AndromedaAlerts

/*
 * Trail Sense's dialogs, drawn in Compose so they look like the rest of the app (28 dp corners, a headline title, the
 * app's colors and type). They keep Andromeda's names and parameters, so callers open them imperatively from any
 * code with a Context; each dialog is a Compose Dialog that the activity's AppHost draws.
 */

/**
 * Shows [content] as a dialog and returns it, so callers can still dismiss it. [onCancel] runs for back, a tap
 * outside (when allowed), or the cancel button's own handling.
 */
fun showComposeDialog(
    context: Context,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    onCancel: () -> Unit = {},
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
): TrailSenseDialog {
    return AppHost.from(context).dialogs.show(onCancel = onCancel) { dialog ->
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
    if (text is Spanned || allowLinks) {
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
            TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) { Text(cancelText.toString()) }
        }
        if (okText != null) {
            Button(onClick = onOk, enabled = okEnabled, shapes = ButtonDefaults.shapes()) {
                Text(okText.toString())
            }
        }
    }
}

/**
 * A caller's view inside a dialog: only views made with [TrailSenseComposeRootView] are supported, and the dialog
 * draws their Compose content directly. New code passes `body` instead.
 */
@Composable
private fun DialogView(view: View, modifier: Modifier = Modifier) {
    val composeContent = remember(view) { composeContentOf(view) }
    if (composeContent == null) {
        Log.e("TrailSenseDialogs", "A dialog's contentView must come from TrailSenseComposeRootView; pass body instead")
        return
    }
    Box(modifier.fillMaxWidth()) { composeContent() }
}

object Alerts {

    /**
     * A dialog with a [title], an optional [content] message, and an optional [body] drawn below it. [contentView]
     * is the older way to pass a body; prefer [body].
     */
    fun dialog(
        context: Context,
        title: CharSequence,
        content: CharSequence? = null,
        contentView: View? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
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
        val hasBody = contentView != null || body != null
        return showComposeDialog(
            context,
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
                if (contentView != null) DialogView(contentView)
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

    fun toast(context: Context, text: CharSequence, short: Boolean = true) =
        AndromedaAlerts.toast(context, text, short)

    /** A dialog with [title] and a spinner that can't be dismissed by the person; dismiss it when the work is done. */
    fun loading(context: Context, title: CharSequence): TrailSenseDialog {
        return showComposeDialog(context, cancelable = false, cancelOnOutsideTouch = false) {
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
    fun progress(context: Context, title: CharSequence): Pair<TrailSenseDialog, (Float) -> Unit> {
        val progress = mutableFloatStateOf(0f)
        val dialog = showComposeDialog(context, cancelable = false, cancelOnOutsideTouch = false) {
            DialogTitle(title)
            ProgressBody(progress = { progress.floatValue })
            Spacer(Modifier.size(TrailSenseSpacing.xl))
        }
        return dialog to { value -> onMain { progress.floatValue = value.coerceIn(0f, 1f) } }
    }

    inline fun <T> withLoading(context: Context, title: CharSequence, action: () -> T): T {
        val loading = loading(context, title)
        try {
            return action()
        } finally {
            loading.dismiss()
        }
    }

    inline fun <T> withProgress(context: Context, title: CharSequence, action: ((Float) -> Unit) -> T): T {
        val (dialog, setProgress) = progress(context, title)
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
        context: Context,
        title: CharSequence,
        content: CharSequence? = null,
        contentView: View? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        allowLinks: Boolean = false,
        cancelable: Boolean = true,
        cancelOnOutsideTouch: Boolean = true,
        scrollable: Boolean = false,
        body: (@Composable () -> Unit)? = null
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val dialog = Alerts.dialog(
            context, title, content, contentView, okText, cancelText, allowLinks, cancelable,
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
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndex: Int = -1,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        onPick: (selectedIndex: Int?) -> Unit
    ) {
        var done = false
        fun pick(index: Int?) {
            if (done) return
            done = true
            onPick(index)
        }
        showComposeDialog(context, onCancel = { pick(null) }) { dismiss ->
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
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndices: List<Int> = emptyList(),
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        onPick: (selectedIndices: List<Int>?) -> Unit
    ) {
        var done = false
        fun pick(indices: List<Int>?) {
            if (done) return
            done = true
            onPick(indices)
        }
        showComposeDialog(context, onCancel = { pick(null) }) { dismiss ->
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
        context: Context,
        title: CharSequence,
        description: CharSequence? = null,
        default: String? = null,
        hint: CharSequence? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        onTextPick: (text: String?) -> Unit
    ) {
        inputDialog(context, title, description, default, hint, KeyboardType.Text, okText, cancelText) {
            onTextPick(it)
        }
    }

    fun number(
        context: Context,
        title: CharSequence,
        description: CharSequence? = null,
        default: Number? = null,
        allowDecimals: Boolean = true,
        allowNegative: Boolean = false,
        hint: CharSequence? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        onNumberPick: (number: Number?) -> Unit
    ) {
        val start = default?.let { if (allowDecimals) it.toString() else it.toLong().toString() }
        inputDialog(
            context, title, description, start, hint,
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
        context: Context,
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
        showComposeDialog(context, cancelOnOutsideTouch = false, onCancel = { pick(null) }) { dismiss ->
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
        context: Context,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        var done = false
        fun pick(time: LocalTime?) {
            if (done) return
            done = true
            onTimePick(time)
        }
        showComposeDialog(context, onCancel = { pick(null) }) { dismiss ->
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
                okText = context.getString(android.R.string.ok),
                cancelText = context.getString(android.R.string.cancel),
                onOk = {
                    dismiss()
                    pick(LocalTime.of(state.hour, state.minute))
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
        context: Context,
        default: LocalDate = LocalDate.now(),
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        var done = false
        fun pick(date: LocalDate?) {
            if (done) return
            done = true
            onDatePick(date)
        }
        val host = AppHost.from(context).dialogs
        host.show(onCancel = { pick(null) }) { dialog ->
            val state = rememberDatePickerState(
                initialSelectedDateMillis = default.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { dialog.cancel() },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.dismiss()
                            pick(state.selectedDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            } ?: default)
                        },
                        shapes = ButtonDefaults.shapes()
                    ) { Text(context.getString(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { dialog.cancel() }, shapes = ButtonDefaults.shapes()) {
                        Text(context.getString(android.R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }

    fun datetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        date(context, default.toLocalDate()) { date ->
            if (date == null) {
                onDatetimePick(null)
                return@date
            }
            time(context, use24Hours, default.toLocalTime()) { time ->
                onDatetimePick(time?.let { LocalDateTime.of(date, it) })
            }
        }
    }

    /**
     * A menu of [items] (null items are left out) at the top end of the screen; [onSelection] gets the index of the
     * chosen item.
     */
    fun menu(context: Context, items: List<CharSequence?>, onSelection: (index: Int) -> Boolean) {
        AppHost.from(context).dialogs.show { dialog ->
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

    /** The older form: [anchorView] only supplies the Context; prefer the Context overload. */
    fun menu(anchorView: View, items: List<CharSequence?>, onSelection: (index: Int) -> Boolean) =
        menu(anchorView.context, items, onSelection)
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
        context: Context,
        title: CharSequence,
        description: CharSequence? = null,
        default: String? = null,
        hint: CharSequence? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel)
    ): String? = suspendCancellableCoroutine { c ->
        Pickers.text(context, title, description, default, hint, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun number(
        context: Context,
        title: CharSequence,
        description: CharSequence? = null,
        default: Number? = null,
        allowDecimals: Boolean = true,
        allowNegative: Boolean = false,
        hint: CharSequence? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel)
    ): Number? = suspendCancellableCoroutine { c ->
        Pickers.number(context, title, description, default, allowDecimals, allowNegative, hint, okText, cancelText) {
            if (c.isActive) c.resume(it)
        }
    }

    suspend fun item(
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndex: Int = -1,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel)
    ): Int? = suspendCancellableCoroutine { c ->
        Pickers.item(context, title, items, defaultSelectedIndex, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun items(
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        defaultSelectedIndices: List<Int> = emptyList(),
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel)
    ): List<Int>? = suspendCancellableCoroutine { c ->
        Pickers.items(context, title, items, defaultSelectedIndices, okText, cancelText) { if (c.isActive) c.resume(it) }
    }

    suspend fun time(context: Context, use24Hours: Boolean, default: LocalTime = LocalTime.now()): LocalTime? =
        suspendCancellableCoroutine { c ->
            Pickers.time(context, use24Hours, default) { if (c.isActive) c.resume(it) }
        }

    suspend fun date(context: Context, default: LocalDate = LocalDate.now()): LocalDate? =
        suspendCancellableCoroutine { c ->
            Pickers.date(context, default) { if (c.isActive) c.resume(it) }
        }

    suspend fun datetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? = suspendCancellableCoroutine { c ->
        Pickers.datetime(context, use24Hours, default) { if (c.isActive) c.resume(it) }
    }
}
