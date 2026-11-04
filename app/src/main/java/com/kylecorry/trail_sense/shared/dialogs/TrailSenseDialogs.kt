package com.kylecorry.trail_sense.shared.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import com.kylecorry.andromeda.alerts.Alerts as AndromedaAlerts
import com.kylecorry.andromeda.pickers.Pickers as AndromedaPickers

/*
 * Trail Sense's dialogs, drawn in Compose so they look like the rest of the app (28 dp corners, a headline title, the
 * app's colors and type) instead of the platform's View dialogs. They keep Andromeda's names and parameters, so a
 * call site changes only its import; everything not redrawn here passes straight through to Andromeda.
 */

/**
 * Shows [content] as a dialog and returns the hosting [AlertDialog], so callers can still dismiss it. [onCancel] runs
 * for back, a tap outside (when allowed), or the cancel button's own handling.
 */
internal fun showComposeDialog(
    context: Context,
    cancelable: Boolean = true,
    cancelOnOutsideTouch: Boolean = true,
    onCancel: () -> Unit = {},
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
): AlertDialog {
    lateinit var dialog: AlertDialog
    val view = TrailSenseComposeRootView(context, ViewCompositionStrategy.DisposeOnDetachedFromWindow) {
        val outside = remember { MutableInteractionSource() }
        // The window fills the screen so the card can size itself; a tap on the scrim around it cancels.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.32f))
                .clickable(interactionSource = outside, indication = null) {
                    if (cancelable && cancelOnOutsideTouch) dialog.cancel()
                }
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 560.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
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
    dialog = AlertDialog.Builder(context).create().apply {
        setView(view, 0, 0, 0, 0)
        setCancelable(cancelable)
        setCanceledOnTouchOutside(false)
        setOnCancelListener { onCancel() }
    }
    dialog.show()
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // Edge to edge like the app, so the scrim runs under the system bars instead of stopping at a black band.
        WindowCompat.setDecorFitsSystemWindows(this, false)
        addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // The dim is drawn by Compose (the scrim box), so it reaches under the system bars too.
        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        @Suppress("DEPRECATION")
        navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        statusBarColor = Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            isNavigationBarContrastEnforced = false
        }
    }
    return dialog
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

/** Plain text in Compose; styled or linked text (markdown, HTML) in a TextView set in the same type and color. */
@Composable
private fun DialogMessage(text: CharSequence, allowLinks: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.bodyMedium
    val linkColor = MaterialTheme.colorScheme.primary
    if (text is Spanned || allowLinks) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { TextView(it) },
            update = {
                it.text = text
                it.setTextColor(color.toArgb())
                it.setLinkTextColor(linkColor.toArgb())
                it.textSize = style.fontSize.value
                it.setLineSpacing(0f, 1.15f)
                if (allowLinks) it.movementMethod = LinkMovementMethod.getInstance()
            }
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
            TextButton(onClick = onCancel) { Text(cancelText.toString()) }
        }
        if (okText != null) {
            TextButton(onClick = onOk, enabled = okEnabled) {
                Text(okText.toString(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** A legacy view inside a dialog: detached from any old parent first. */
@Composable
private fun DialogView(view: View, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            (view.parent as? ViewGroup)?.removeView(view)
            view
        }
    )
}

object Alerts {

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
        onClose: ((cancelled: Boolean) -> Unit)? = null
    ): AlertDialog {
        var closed = false
        fun close(cancelled: Boolean) {
            if (closed) return
            closed = true
            onClose?.invoke(cancelled)
        }
        return showComposeDialog(
            context,
            cancelable = cancelable,
            cancelOnOutsideTouch = cancelOnOutsideTouch,
            onCancel = { close(true) }
        ) { dismiss ->
            DialogTitle(title)
            // Text scrolls on its own; a custom view keeps its own scrolling unless the caller asked for it.
            val body = if (contentView == null || scrollable) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(body)
                    .padding(horizontal = TrailSenseSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                if (content != null) DialogMessage(content, allowLinks)
                if (contentView != null) DialogView(contentView)
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

    fun loading(context: Context, title: CharSequence) = AndromedaAlerts.loading(context, title)

    fun image(context: Context, title: CharSequence, drawable: Int) = AndromedaAlerts.image(context, title, drawable)

    inline fun <T> withLoading(context: Context, title: CharSequence, action: () -> T): T =
        AndromedaAlerts.withLoading(context, title, action)

    inline fun <T> withProgress(context: Context, title: CharSequence, action: ((Float) -> Unit) -> T): T =
        AndromedaAlerts.withProgress(context, title, action)
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
        scrollable: Boolean = false
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val dialog = Alerts.dialog(
            context, title, content, contentView, okText, cancelText, allowLinks, cancelable,
            cancelOnOutsideTouch, scrollable
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
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    shape = RoundedCornerShape(16.dp)
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

    fun time(
        context: Context,
        use24Hours: Boolean,
        default: java.time.LocalTime = java.time.LocalTime.now(),
        onTimePick: (time: java.time.LocalTime?) -> Unit
    ) = AndromedaPickers.time(context, use24Hours, default, onTimePick)

    fun date(
        context: Context,
        default: java.time.LocalDate = java.time.LocalDate.now(),
        onDatePick: (date: java.time.LocalDate?) -> Unit
    ) = AndromedaPickers.date(context, default, onDatePick)

    fun datetime(
        context: Context,
        use24Hours: Boolean,
        default: java.time.LocalDateTime = java.time.LocalDateTime.now(),
        onDatetimePick: (value: java.time.LocalDateTime?) -> Unit
    ) = AndromedaPickers.datetime(context, use24Hours, default, onDatetimePick)

    fun menu(anchorView: View, items: List<CharSequence?>, onSelection: (index: Int) -> Boolean) =
        AndromedaPickers.menu(anchorView, items, onSelection)

    fun menu(anchorView: View, menu: Int, onSelection: (itemId: Int) -> Boolean) =
        AndromedaPickers.menu(anchorView, menu, onSelection)

    fun menu(anchorView: View, items: List<com.kylecorry.andromeda.pickers.MenuItem>) =
        AndromedaPickers.menu(anchorView, items)
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

    suspend fun time(context: Context, use24Hours: Boolean, default: java.time.LocalTime = java.time.LocalTime.now()) =
        com.kylecorry.andromeda.pickers.CoroutinePickers.time(context, use24Hours, default)

    suspend fun date(context: Context, default: java.time.LocalDate = java.time.LocalDate.now()) =
        com.kylecorry.andromeda.pickers.CoroutinePickers.date(context, default)

    suspend fun datetime(
        context: Context,
        use24Hours: Boolean,
        default: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ) = com.kylecorry.andromeda.pickers.CoroutinePickers.datetime(context, use24Hours, default)

    suspend fun menu(anchorView: View, items: List<CharSequence?>) =
        com.kylecorry.andromeda.pickers.CoroutinePickers.menu(anchorView, items)

    suspend fun menu(anchorView: View, menu: Int) =
        com.kylecorry.andromeda.pickers.CoroutinePickers.menu(anchorView, menu)
}

