package com.kylecorry.trail_sense.shared.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.testTagsAsIds

/** Shows [composeContent] in a dialog with a [title], an optional text [content] above it, and only an OK button. */
fun showComposeContentDialog(
    host: AppHost,
    title: String,
    content: CharSequence? = null,
    composeContent: @Composable () -> Unit
) {
    val view: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTagsAsIds()
                .padding(24.dp)
        ) {
            if (content != null) {
                Text(
                    text = content.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            composeContent()
        }
    }
    Alerts.dialog(host, title, body = view, cancelText = null)
}
