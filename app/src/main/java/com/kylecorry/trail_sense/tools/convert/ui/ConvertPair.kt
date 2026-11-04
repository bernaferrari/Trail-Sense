@file:Suppress("FunctionNaming")
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseChoiceColors

/*
 * Convert in Calcula's language: both values live in one card, one row above the other. The row you type into reads
 * as a field; the other one is the answer, in primary. Tap the answer and it becomes the input, carrying its value.
 * Each row's unit is a pill; tapping it hides the keyboard and puts that category's units in its place. A swap
 * button rides the seam between the rows. Long-press a value to copy it.
 */

/** The two rows and, where it makes sense, the swap button on the seam between them. */
@Composable
internal fun ConvertCard(
    onSwap: (() -> Unit)?,
    swapLabel: String,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit
) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 180f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swap")
    Surface(
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Box {
            Column(
                modifier = Modifier.padding(TrailSenseSpacing.xs + TrailSenseSpacing.xxs),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
                content = rows
            )
            if (onSwap != null) {
                FilledTonalIconButton(
                    onClick = {
                        turns += 1f
                        onSwap()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = TrailSenseSpacing.screen)
                        .size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.SwapVert,
                        contentDescription = swapLabel,
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}

/**
 * One value of the card: its unit pill on top, the number under it, right-aligned. [active] is the row being typed
 * into; [picking] lights its pill while its units are shown; [dimmed] steps the other row back meanwhile.
 */
@Composable
internal fun ConvertRow(
    active: Boolean,
    picking: Boolean,
    dimmed: Boolean,
    unit: String?,
    onPickUnit: (() -> Unit)?,
    onActivate: (() -> Unit)?,
    copyText: String?,
    modifier: Modifier = Modifier,
    leaveRoomForSwap: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val field by animateColorAsState(
        if (active || picking) colors.surfaceContainerHigh else Color.Transparent,
        label = "field"
    )
    val pillColor by animateColorAsState(if (picking) colors.primary else colors.surfaceContainerHighest, label = "pill")
    val pillContent by animateColorAsState(if (picking) colors.onPrimary else colors.onSurface, label = "pillContent")
    val alpha by animateFloatAsState(if (dimmed) 0.38f else 1f, label = "dim")
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(20.dp))
            .background(field)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = { onActivate?.invoke() },
                onLongClick = copyText?.let {
                    {
                        clipboard.setText(AnnotatedString(it))
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }
            )
            .padding(
                start = if (leaveRoomForSwap) 88.dp else TrailSenseSpacing.lg,
                end = TrailSenseSpacing.screen,
                top = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.md
            )
    ) {
        if (unit != null) {
            if (onPickUnit != null) {
                Surface(onClick = onPickUnit, shape = CircleShape, color = pillColor, contentColor = pillContent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .padding(start = 14.dp, end = TrailSenseSpacing.sm)
                    ) {
                        Text(
                            unit,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            if (picking) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.heightIn(min = 36.dp).padding(top = TrailSenseSpacing.sm)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** The family's number style for a converter value; long values step down so they stay on one line. */
@Composable
internal fun convertNumberStyle(text: String): TextStyle {
    val base = when {
        text.length > 22 -> MaterialTheme.typography.titleLarge
        text.length > 14 -> MaterialTheme.typography.headlineSmall
        text.length > 9 -> MaterialTheme.typography.headlineLarge
        else -> MaterialTheme.typography.displaySmall
    }
    return base.copy(fontFeatureSettings = "tnum", textAlign = TextAlign.End)
}

/** The value being typed, on the system keyboard. Shows a faint 0 (or [placeholder]) while empty. */
@Composable
internal fun RowScope.ConvertInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    keyboardType: KeyboardType,
    placeholder: String,
    isError: Boolean = false,
    onDone: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val style = convertNumberStyle(value.ifEmpty { placeholder })
    // A value set from outside (the answer carried over, a paste) puts the cursor after it, ready to edit.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) {
        field = TextFieldValue(value, TextRange(value.length))
    }
    BasicTextField(
        value = field,
        onValueChange = {
            field = it
            onValueChange(it.text)
        },
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = style.copy(color = if (isError) colors.error else colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = style,
                        color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                inner()
            }
        }
    )
}

/** The answer: in primary, so it reads as the result rather than something to edit. */
@Composable
internal fun RowScope.ConvertValue(text: String, placeholder: String = "0") {
    val colors = MaterialTheme.colorScheme
    Text(
        text.ifEmpty { placeholder },
        style = convertNumberStyle(text.ifEmpty { placeholder }),
        color = if (text.isEmpty()) colors.onSurfaceVariant.copy(alpha = 0.4f) else colors.primary,
        modifier = Modifier.weight(1f)
    )
}

/** A unit's symbol beside its number, quieter than the number. */
@Composable
internal fun ConvertSymbol(symbol: String) {
    if (symbol.isBlank()) return
    Text(
        symbol,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.padding(start = TrailSenseSpacing.sm)
    )
}

/** One choice of a [ConvertUnitGrid]: its short symbol (when it has one) over its name. */
internal data class ConvertUnitOption<T>(val value: T, val name: String, val symbol: String?)

/** The units of the category, in tiles, where the keyboard was. The chosen one is solid primary. */
@Composable
internal fun <T> ConvertUnitGrid(
    options: List<ConvertUnitOption<T>>,
    selected: T,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val gap = TrailSenseSpacing.xs + TrailSenseSpacing.xxs
    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { option ->
                    val checked = option.value == selected
                    ToggleButton(
                        checked = checked,
                        // Picking the current unit also closes the picker.
                        onCheckedChange = { onPick(option.value) },
                        colors = trailSenseChoiceColors(),
                        contentPadding = PaddingValues(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.sm),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (option.symbol != null) 64.dp else 56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (option.symbol != null) {
                                Text(
                                    option.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1
                                )
                            }
                            Text(
                                option.name,
                                style = if (option.symbol != null) {
                                    MaterialTheme.typography.labelSmall
                                } else {
                                    MaterialTheme.typography.labelLarge
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Swap's label, shared by the converters that can swap. */
@Composable
internal fun convertSwapLabel(): String = stringResource(R.string.convert_swap_units)
