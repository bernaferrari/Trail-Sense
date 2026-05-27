package com.kylecorry.trail_sense.tools.diagnostics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.compose.useFlow
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val tools = remember { Tools.getTools(context) }
    val diagnostics = remember(tools) {
        tools.flatMap { it.diagnostics }.distinctBy { it.id }
    }
    val diagnosticIdToTool = remember(tools) {
        val map = mutableMapOf<String, List<Tool>>()
        tools.forEach { tool ->
            tool.diagnostics.forEach { diag ->
                map[diag.id] = map.getOrDefault(diag.id, emptyList()) + tool
            }
        }
        map
    }

    val resultsFlow: Flow<Map<String, List<ToolDiagnosticResult>>> = useMemo(context, diagnostics) {
        if (diagnostics.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val flows = diagnostics.map { it.scanner.fullScan(context) }
            combine(flows) { resultsArray ->
                diagnostics.mapIndexed { index, diag ->
                    diag.id to resultsArray[index]
                }.toMap()
            }
        }
    }
    val results = useFlow(resultsFlow) ?: emptyMap()

    val items = useMemo(results, diagnosticIdToTool) {
        results.flatMap { (id, list) ->
            list.map { DiagnosticItem(it, diagnosticIdToTool[id] ?: emptyList()) }
        }.toSet().sortedBy { it.result.severity.ordinal }
    }

    val (selectedItem, setSelectedItem) = useState<DiagnosticItem?>(null)

    selectedItem?.let { item ->
        DiagnosticDetailDialog(
            item = item,
            onDismiss = { setSelectedItem(null) }
        )
    }

    DiagnosticsList(
        items = items,
        onItemClick = { setSelectedItem(it) },
        modifier = modifier
    )
}

@Composable
fun DiagnosticsList(
    items: List<DiagnosticItem>,
    onItemClick: (DiagnosticItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        DiagnosticsEmptyState(modifier = modifier.fillMaxSize())
        return
    }

    val warningPainter: Painter = painterResource(R.drawable.ic_alert)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrailSenseSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        items(items, key = { it.result.id }) { item ->
            TrailSenseCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                TrailSenseListItem(
                    icon = warningPainter,
                    title = item.result.name,
                    subtitle = item.result.description,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(TrailSenseSpacing.sm)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticDetailDialog(
    item: DiagnosticItem,
    onDismiss: () -> Unit
) {
    val affectedToolsText = item.tools.sortedBy { it.name }
        .joinToString("\n") { "\u2022 ${it.name}" }
    val resolution = item.result.resolution
        ?: stringResource(R.string.no_resolution)

    val message = stringResource(
        R.string.diagnostic_message_template,
        getSeverityLabel(item.result.severity),
        item.result.description,
        affectedToolsText,
        resolution
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.result.name) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
private fun getSeverityLabel(severity: ToolDiagnosticSeverity): String {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> stringResource(R.string.error)
        ToolDiagnosticSeverity.Warning -> stringResource(R.string.warning)
    }
}

@Composable
private fun DiagnosticsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(
            horizontal = TrailSenseSpacing.xxl,
            vertical = TrailSenseSpacing.xxxl
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(TrailSenseSpacing.xl))
        Text(
            text = stringResource(R.string.no_issues),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
