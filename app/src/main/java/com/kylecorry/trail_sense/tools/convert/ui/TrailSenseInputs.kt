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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/*
 * Form inputs for the utility tools (convert, ballistics, temperature estimation, declination). They take a group
 * position like the other rows, so a field sits inside a section with the 24/6 silhouette instead of an outline.
 * Lives with the converter until shared/views/compose is free to take it.
 */

/**
 * A filled, borderless text field shaped as one row of a group. A unit, when given, is a tonal button at the end
 * that opens the unit choice, so the number and what it's measured in read as one thing.
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
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        isError = isError,
        singleLine = true,
        leadingIcon = leadingIcon,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = trailSenseGroupShape(position),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
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

/** The unit chip at the end of a [TrailSenseInputField]: the unit's short name and a drop-down mark. */
@Composable
fun TrailSenseUnitButton(
    unit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 40.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TrailSenseSpacing.md,
            end = TrailSenseSpacing.sm
        )
    ) {
        Text(unit, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
