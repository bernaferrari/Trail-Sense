package com.kylecorry.andromeda.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FormColumn(modifier: Modifier = Modifier, scrollable: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val scroll = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(scroll, verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun FormTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, placeholder: String? = null,
    enabled: Boolean = true, singleLine: Boolean = true, error: String? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } },
        enabled = enabled, singleLine = singleLine, isError = error != null,
        supportingText = error?.let { { Text(it) } }
    )
}

@Composable
fun FormNumberField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, allowDecimals: Boolean = true, allowNegative: Boolean = false,
    enabled: Boolean = true, placeholder: String? = null, error: String? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } },
        enabled = enabled, singleLine = true, isError = error != null, supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (allowNegative) KeyboardType.Text else if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number)
    )
}

@Composable
fun FormSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun FormCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun <T> FormDropdown(
    selected: T?, options: List<T>, label: String, optionLabel: (T) -> String,
    onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    selectedLabel: (T) -> String = optionLabel
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let(selectedLabel) ?: label)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { expanded = false; onSelected(option) })
                }
            }
        }
    }
}

@Composable
fun FormButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: ButtonColors = ButtonDefaults.buttonColors()) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) { Text(label) }
}

@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) { Text(text, modifier) }

@Composable
fun FormLoading(modifier: Modifier = Modifier) { CircularProgressIndicator(modifier) }

@Composable
fun <T> FormUnitField(
    value: String, onValueChange: (String) -> Unit, unit: T?, units: List<T>,
    label: String, unitLabel: (T) -> String, onUnitChange: (T) -> Unit, modifier: Modifier = Modifier,
    unitShortLabel: (T) -> String = unitLabel, enabled: Boolean = true, placeholder: String? = null
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FormNumberField(value, onValueChange, label, modifier = Modifier.weight(1f), allowNegative = true, enabled = enabled, placeholder = placeholder)
        FormDropdown(unit, units, label, unitLabel, onUnitChange, modifier = Modifier.weight(1f), enabled = enabled, selectedLabel = unitShortLabel)
    }
}
