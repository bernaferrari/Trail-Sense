@file:Suppress("FunctionNaming", "LongMethod")

package com.kylecorry.trail_sense.main.errors

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog

@Composable
fun ErrorHandlerScreen(
    toolId: Long,
    onEmail: () -> Unit,
    onCopyError: () -> Unit,
    onViewDetails: () -> Unit,
    onReopenTool: () -> Unit,
    onRestartApp: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val toolName = getAppService<ToolCatalog>().getTools().firstOrNull { it.id == toolId }?.name
        ?: appString(Res.string.app_name)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TrailSenseSpacing.screen, vertical = TrailSenseSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.padding(TrailSenseSpacing.lg)
            )
        }
        Spacer(Modifier.height(TrailSenseSpacing.lg))
        Text(
            text = appString(Res.string.error_occurred),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(TrailSenseSpacing.sm))
        Text(
            text = toolName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(TrailSenseSpacing.xxl))
        TrailSenseButton(
            text = appString(Res.string.reopen_tool),
            onClick = onReopenTool,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        )
        Spacer(Modifier.height(TrailSenseSpacing.sm))
        TrailSenseButton(
            text = appString(Res.string.restart_app),
            onClick = onRestartApp,
            icon = Icons.Default.Refresh,
            style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        )
        Spacer(Modifier.height(TrailSenseSpacing.md))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ErrorTextAction(
                text = appString(Res.string.view_error_details),
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = onViewDetails
            )
            ErrorTextAction(
                text = appString(Res.string.copy_error),
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = onCopyError
            )
            ErrorTextAction(
                text = appString(Res.string.pref_email_title),
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                onClick = onEmail
            )
        }
        TrailSenseButton(
            text = appString(Res.string.settings),
            onClick = onOpenSettings,
            icon = Icons.Default.Settings,
            style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Text
        )
    }
}

@Composable
private fun ErrorTextAction(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 88.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(TrailSenseSpacing.xxs))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

