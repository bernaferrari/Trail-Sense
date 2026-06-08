package com.kylecorry.trail_sense.shared.views

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
