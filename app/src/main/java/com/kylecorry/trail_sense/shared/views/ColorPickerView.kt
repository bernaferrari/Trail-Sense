package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun ColorPickerDialog(
    title: String,
    selectedColor: AppColor?,
    onColorPicked: (AppColor?) -> Unit,
    onDismiss: () -> Unit
) {
    var color by remember(selectedColor) { mutableStateOf(selectedColor) }

    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = androidx.compose.ui.res.stringResource(android.R.string.ok),
        onConfirm = { onColorPicked(color) },
        dismissText = androidx.compose.ui.res.stringResource(android.R.string.cancel)
    ) {
        ColorPickerContent(
            selectedColor = color,
            onColorSelected = {
                color = it
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerContent(
    selectedColor: AppColor?,
    onColorSelected: (AppColor) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("color_picker"),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        AppColor.values().forEach { color ->
            ColorSwatch(
                color = color,
                selected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: AppColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = color.name }
            .testTag("color_${color.name}"),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(color.color), CircleShape)
        )
    }
}
