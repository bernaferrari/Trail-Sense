package com.kylecorry.trail_sense.shared.map_layers.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.toAnnotatedString

class MapAttributionState {

    var text by mutableStateOf<CharSequence?>(null)
    var isVisible by mutableStateOf(false)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
        val attribution = text ?: return
        val annotated = remember(attribution) { attribution.toAnnotatedString(linkColor = Color.White) }

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = Color(0xB8000000)
        ) {
            Text(
                text = annotated,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White
                )
            )
        }
    }
}
