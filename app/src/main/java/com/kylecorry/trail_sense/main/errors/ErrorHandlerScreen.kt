@file:Suppress("FunctionNaming", "LongMethod")

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
internal fun ErrorHandlerScreen(
    toolId: Long,
    onEmail: () -> Unit,
    onCopyError: () -> Unit,
    onViewDetails: () -> Unit,
    onReopenTool: () -> Unit,
    onRestartApp: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val toolName = Tools.getTools(context).firstOrNull { it.id == toolId }?.name
        ?: stringResource(R.string.app_name)

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
            onClick = onEmail
        )
        ErrorActionButton(
            text = stringResource(R.string.copy_error),
            icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            onClick = onCopyError
        )
        ErrorActionButton(
            text = stringResource(R.string.view_error_details),
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = onViewDetails
        )
        ErrorActionButton(
            text = stringResource(R.string.reopen_tool),
            icon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
            onClick = onReopenTool
        )
        ErrorActionButton(
            text = stringResource(R.string.restart_app),
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            onClick = onRestartApp
        )
        ErrorActionButton(
            text = stringResource(R.string.settings),
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = onOpenSettings
        )
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
