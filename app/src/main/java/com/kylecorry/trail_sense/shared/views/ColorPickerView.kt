package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.colors.AppColor

class ColorPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var changeListener: ((color: AppColor?) -> Unit)? = null
    private var selectedColor by mutableStateOf<AppColor?>(null)

    var color: AppColor?
        get() = selectedColor
        set(value) {
            selectedColor = value
        }

    init {
        addView(
            ComposeView(context).apply {
                setContent {
                    TrailSenseTheme {
                        ColorPickerContent(
                            selectedColor = selectedColor,
                            onColorSelected = {
                                color = it
                                changeListener?.invoke(it)
                            }
                        )
                    }
                }
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun setOnColorChangeListener(listener: ((color: AppColor?) -> Unit)?) {
        changeListener = listener
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerContent(
    selectedColor: AppColor?,
    onColorSelected: (AppColor) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("color_picker")
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
