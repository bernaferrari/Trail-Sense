package com.kylecorry.trail_sense.shared.views

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class UnitPickerButton(context: Context) : AbstractComposeView(context) {
    var text by mutableStateOf("")
    private var buttonEnabled by mutableStateOf(isEnabled)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        buttonEnabled = enabled
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Button(
                onClick = { performClick() },
                enabled = buttonEnabled
            ) {
                Text(text = text)
            }
        }
    }
}
