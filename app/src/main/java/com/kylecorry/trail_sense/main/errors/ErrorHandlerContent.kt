package com.kylecorry.trail_sense.main.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.compose.useArgument
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/**
 * Pretty Compose version of the error handler screen.
 */
@Composable
fun Fragment.ErrorHandlerContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navController = useNavController()

    val toolId by useArgument<Long>("tool_id") ?: 0L
    val errorDetails by useArgument<String>("error") ?: ""

    val tool = remember(toolId) {
        Tools.getTools(context).firstOrNull { it.id == toolId }
    }
    val toolName = tool?.name ?: stringResource(R.string.app_name)

    var showDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.error_occurred),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = toolName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        ActionButton(
            text = stringResource(R.string.pref_email_title),
            onClick = {
                val intent = Intents.email(
                    stringResource(R.string.email),
                    "Error in ${stringResource(R.string.app_name)}",
                    errorDetails
                )
                startActivity(intent)
            }
        )

        ActionButton(
            text = stringResource(R.string.copy_error),
            onClick = {
                Clipboard.copy(context, errorDetails, stringResource(R.string.copied_to_clipboard_toast))
            }
        )

        ActionButton(
            text = stringResource(R.string.view_error_details),
            onClick = { showDetails = true }
        )

        ActionButton(
            text = stringResource(R.string.reopen_tool),
            onClick = { navController.openTool(toolId) }
        )

        ActionButton(
            text = stringResource(R.string.restart_app),
            onClick = { CurrentApp.restart(context) }
        )

        ActionButton(
            text = stringResource(R.string.settings),
            onClick = { navController.openTool(Tools.SETTINGS) }
        )
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(stringResource(R.string.error_occurred)) },
            text = { Text(errorDetails) },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors()
    ) {
        Text(text, modifier = Modifier.padding(vertical = 4.dp))
    }
}
