@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app's one Compose dialog, matching Alerts.dialog: a plain Material AlertDialog (its own shape and colours), a
 * title, any content, and the buttons at the end: a text button to back out ([dismissText]) and a filled one to go
 * ahead ([confirmText]). A dialog that only informs passes just [confirmText] ("OK").
 */
@Composable
fun TrailSenseDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    content: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title?.let { { Text(it) } },
        text = content,
        confirmButton = {
            if (confirmText != null) {
                Button(onClick = onConfirm, enabled = confirmEnabled, shapes = ButtonDefaults.shapes()) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(it)
                }
            }
        }
    )
}
