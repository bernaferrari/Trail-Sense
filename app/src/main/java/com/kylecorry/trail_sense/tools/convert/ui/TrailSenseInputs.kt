package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.text.input.TextFieldValue

/*
 * Form inputs for the utility tools (convert, ballistics, temperature estimation, declination). They take a group
 * position like the other rows, so a field sits inside a section with the 24/6 silhouette instead of an outline.
 * Lives with the converter until shared/views/compose is free to take it.
 */

/**
 * The app's one text field: filled, borderless, shaped as one row of a group. A unit, when given, is a tonal button
 * at the end that opens the unit choice, so the number and what it's measured in read as one thing. Fields laid out
 * side by side inside a card pass [inCard] (a raised tone and 16 dp corners, so they show against the card).
 */
@Composable
fun TrailSenseInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    unit: String? = null,
    onUnitClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    /** Makes the field a read-only picker: tapping it calls this (to open a choice) instead of typing. */
    onClick: (() -> Unit)? = null,
    /** False for free text that wraps (a note); [minLines] then sets its resting height. */
    singleLine: Boolean = true,
    minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    /** A quiet line under the field; [errorText] takes its place while [isError]. */
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Default,
    inCard: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    if (onClick != null) {
        val currentOnClick by rememberUpdatedState(onClick)
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                if (it is PressInteraction.Release) currentOnClick()
            }
        }
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        readOnly = onClick != null,
        interactionSource = interactionSource,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        supportingText = when {
            isError && errorText != null -> {
                { Text(errorText) }
            }
            supportingText != null -> {
                { Text(supportingText) }
            }
            else -> null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        shape = if (inCard) RoundedCornerShape(16.dp) else trailSenseGroupShape(position),
        colors = trailSenseInputColors(inCard),
        trailingIcon = when {
            unit != null && onUnitClick != null -> {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        trailingIcon?.invoke()
                        TrailSenseUnitButton(
                            unit = unit,
                            onClick = onUnitClick,
                            enabled = enabled,
                            modifier = Modifier.padding(end = TrailSenseSpacing.sm)
                        )
                    }
                }
            }

            unit != null -> {
                {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = TrailSenseSpacing.lg)
                    )
                }
            }

            else -> trailingIcon
        }
    )
}

/**
 * [TrailSenseInputField] for a caller that owns the cursor (a [TextFieldValue]), such as a field that reformats
 * what is typed and keeps the cursor at the end.
 */
@Composable
fun TrailSenseInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        isError = isError,
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = trailSenseGroupShape(position),
        colors = trailSenseInputColors()
    )
}

/** The same tone as the list rows, so a field and a row in one group read as one object; no indicator line. */
@Composable
private fun trailSenseInputColors(inCard: Boolean = false): TextFieldColors {
    val container = if (inCard) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    return TextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    )
}

/**
 * The unit button at the end of a [TrailSenseInputField]: the unit's short name and a drop-down mark.
 */
@Composable
fun TrailSenseUnitButton(
    unit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Text then a drop-down mark, so not a kit button: a plain tonal Material button with its own padding.
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(unit, maxLines = 1)
        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/**
 * Picks one of a short list: a dialog of radio rows. Choosing closes it, so there is no confirm button, only a
 * way out.
 */
@Composable
fun <T> TrailSenseOptionsDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        dismissText = stringResource(android.R.string.cancel)
    ) {
        Column(
            modifier = Modifier
                .selectableGroup()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            options.forEach { (name, value) ->
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TrailSenseSize.minimumTouchTarget)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(value) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
