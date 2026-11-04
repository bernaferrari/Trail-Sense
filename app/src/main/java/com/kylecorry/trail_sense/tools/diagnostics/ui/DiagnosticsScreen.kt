@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.diagnostics.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseHeroCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long to wait for every scanner to report before the screen stops saying it's still checking. */
private const val DIAGNOSTICS_SETTLE_MILLIS = 3000L

/**
 * Diagnostics: a hero with the one answer (all good, or how many things need attention), the issues grouped by
 * severity, then the two things you can do here (sensor details, export logs). The all-clear never shows before the
 * scanners have had a chance to report.
 */
@Composable
internal fun DiagnosticsScreen(
    onShowSensorDetails: () -> Unit,
    onShowDiagnostic: (DiagnosticItem) -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val tools = remember { Tools.getTools(context) }
    val diagnosticIdToTool = remember(tools) {
        val toolMap = mutableMapOf<String, List<Tool>>()
        tools.forEach {
            it.diagnostics.forEach { diagnostic ->
                toolMap[diagnostic.id] = toolMap.getOrDefault(diagnostic.id, listOf()) + it
            }
        }
        toolMap
    }
    val toolDiagnostics = remember(tools) {
        tools.flatMap { it.diagnostics }.distinctBy { it.id }
    }
    var results by remember { mutableStateOf<Map<String, List<ToolDiagnosticResult>>>(emptyMap()) }
    var settled by remember { mutableStateOf(false) }

    LaunchedEffect(toolDiagnostics) {
        toolDiagnostics.forEach { diagnostic ->
            launch {
                diagnostic.scanner.fullScan(context).collect {
                    results = results + (diagnostic.id to it)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(DIAGNOSTICS_SETTLE_MILLIS)
        settled = true
    }

    val items = remember(results, diagnosticIdToTool) {
        results.flatMap { (id, diagnosticResults) ->
            diagnosticResults.map { DiagnosticItem(it, diagnosticIdToTool[id] ?: listOf()) }
        }.toSet().sortedBy { it.result.severity.ordinal }
    }
    val loaded = settled || results.size >= toolDiagnostics.size
    val errors = items.filter { it.result.severity == ToolDiagnosticSeverity.Error }
    val warnings = items.filter { it.result.severity == ToolDiagnosticSeverity.Warning }

    TrailSensePage(
        title = stringResource(R.string.diagnostics),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("diagnostics_list")
    ) {
        item(key = "hero") {
            DiagnosticsHero(
                loaded = loaded,
                errorCount = errors.size,
                warningCount = warnings.size
            )
        }

        diagnosticsSection("errors", R.string.errors, errors, onShowDiagnostic)
        diagnosticsSection("warnings", R.string.diagnostics_warnings, warnings, onShowDiagnostic)

        trailSenseSection(key = "actions") {
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.pref_sensor_details_title),
                    subtitle = stringResource(R.string.diagnostics_sensor_details_summary),
                    icon = rememberVectorPainter(Icons.Default.Sensors),
                    position = position,
                    onClick = onShowSensorDetails,
                    modifier = Modifier.testTag("diagnostics_title"),
                    trailing = { DiagnosticsChevron() }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = stringResource(R.string.export_logs),
                    subtitle = stringResource(R.string.diagnostics_export_logs_summary),
                    icon = rememberVectorPainter(Icons.Default.Download),
                    position = position,
                    onClick = onExportLogs,
                    modifier = Modifier.testTag("download_logs")
                )
            }
        }
    }
}

private fun LazyListScope.diagnosticsSection(
    key: String,
    title: Int,
    items: List<DiagnosticItem>,
    onShowDiagnostic: (DiagnosticItem) -> Unit
) {
    if (items.isEmpty()) return
    item(key = key) {
        TrailSenseRowGroup(
            title = stringResource(title),
            modifier = Modifier.animateItem()
        ) {
            items.forEach { item ->
                row { position ->
                    DiagnosticListItem(
                        item = item,
                        position = position,
                        onClick = { onShowDiagnostic(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsHero(loaded: Boolean, errorCount: Int, warningCount: Int) {
    val issueCount = errorCount + warningCount
    val (container, content) = when {
        errorCount > 0 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        warningCount > 0 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        loaded -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    val containerColor by animateColorAsState(
        container,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "diagnosticsHeroContainer"
    )
    val contentColor by animateColorAsState(
        content,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "diagnosticsHeroContent"
    )
    val icon = when {
        errorCount > 0 -> Icons.Default.ErrorOutline
        warningCount > 0 -> Icons.Default.WarningAmber
        loaded -> Icons.Default.CheckCircle
        else -> Icons.Default.HourglassEmpty
    }
    val title = when {
        issueCount > 0 -> pluralStringResource(R.plurals.diagnostics_issue_count, issueCount, issueCount)
        loaded -> stringResource(R.string.no_issues)
        else -> stringResource(R.string.diagnostics_checking)
    }
    val supporting = when {
        issueCount > 0 -> stringResource(R.string.diagnostics_issues_summary)
        loaded -> stringResource(R.string.diagnostics_no_issues_summary)
        else -> null
    }
    TrailSenseHeroCard(
        title = title,
        supportingText = supporting,
        icon = rememberVectorPainter(icon),
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .then(if (issueCount == 0 && loaded) Modifier.testTag("empty_text") else Modifier)
    )
}

@Composable
private fun DiagnosticListItem(
    item: DiagnosticItem,
    position: TrailSenseGroupPosition,
    onClick: () -> Unit
) {
    val severity = item.result.severity
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "diagnosticPress"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = trailSenseGroupShape(position),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                color = statusContainerColor(severity),
                contentColor = onStatusContainerColor(severity),
                modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = statusIcon(severity),
                        contentDescription = stringResource(
                            if (severity == ToolDiagnosticSeverity.Error) R.string.error else R.string.warning
                        ),
                        modifier = Modifier.size(TrailSenseSize.leadingIcon)
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = item.result.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.result.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(TrailSenseSpacing.sm))
            DiagnosticsChevron()
        }
    }
}

@Composable
private fun DiagnosticsChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun statusContainerColor(severity: ToolDiagnosticSeverity): Color {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> MaterialTheme.colorScheme.errorContainer
        ToolDiagnosticSeverity.Warning -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

@Composable
private fun onStatusContainerColor(severity: ToolDiagnosticSeverity): Color {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> MaterialTheme.colorScheme.onErrorContainer
        ToolDiagnosticSeverity.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
    }
}

private fun statusIcon(severity: ToolDiagnosticSeverity): ImageVector {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> Icons.Default.ErrorOutline
        ToolDiagnosticSeverity.Warning -> Icons.Default.WarningAmber
    }
}
