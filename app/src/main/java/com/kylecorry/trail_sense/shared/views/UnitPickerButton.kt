package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class UnitPickerButtonView(
    context: Context,
    attrs: AttributeSet? = null,
    val state: UnitPickerButtonState = UnitPickerButtonState()
) : AbstractComposeView(context, attrs) {

    @Composable
    override fun Content() {
        TrailSenseTheme {
            state.Content()
        }
    }
}

class UnitPickerButtonState {
    var text by mutableStateOf("")
    var isEnabled by mutableStateOf(true)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content() {
        Button(
            onClick = { onClick?.invoke() },
            enabled = isEnabled
        ) {
            Text(text = text)
        }
    }
}
