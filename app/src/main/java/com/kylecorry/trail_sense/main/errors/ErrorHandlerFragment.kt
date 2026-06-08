package com.kylecorry.trail_sense.main.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.CurrentApp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ErrorHandlerFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val toolId = arguments?.getLong("tool_id") ?: 0L
        val errorDetails = arguments?.getString("error").orEmpty()
        val toolName = Tools.getTools(requireContext()).firstOrNull { it.id == toolId }?.name
            ?: stringResource(R.string.app_name)
        val navController = findNavController()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(TrailSenseSpacing.md))
            Text(
                text = stringResource(R.string.error_occurred),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(TrailSenseSpacing.sm))
            Text(
                text = toolName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            ErrorActionButton(
                text = stringResource(R.string.pref_email_title),
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                onClick = {
                    startActivity(
                        Intents.email(
                            getString(R.string.email),
                            "Error in ${getString(R.string.app_name)}",
                            errorDetails
                        )
                    )
                }
            )
            ErrorActionButton(
                text = stringResource(R.string.copy_error),
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    Clipboard.copy(
                        requireContext(),
                        errorDetails,
                        getString(R.string.copied_to_clipboard_toast)
                    )
                }
            )
            ErrorActionButton(
                text = stringResource(R.string.view_error_details),
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = {
                    Alerts.dialog(
                        requireContext(),
                        getString(R.string.error_occurred),
                        errorDetails,
                        cancelText = null
                    )
                }
            )
            ErrorActionButton(
                text = stringResource(R.string.reopen_tool),
                icon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                onClick = { navController.openTool(toolId) }
            )
            ErrorActionButton(
                text = stringResource(R.string.restart_app),
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                onClick = { CurrentApp.restart(requireContext()) }
            )
            ErrorActionButton(
                text = stringResource(R.string.settings),
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = { navController.openTool(Tools.SETTINGS) }
            )
        }
    }
}

@Composable
private fun ErrorActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        icon()
        Spacer(Modifier.width(TrailSenseSpacing.sm))
        Text(text)
    }
}
