@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseValueChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
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

    val toolsTitle = stringResource(R.string.sky_diagnostics_tools)
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
                warningCount = warnings.size,
                checks = toolDiagnostics.size,
                reported = results.size
            )
        }

        // The counts live in the hero's severity bar rather than a second row of tiles.

        diagnosticsSection("errors", R.string.errors, errors, onShowDiagnostic)
        diagnosticsSection("warnings", R.string.diagnostics_warnings, warnings, onShowDiagnostic)

        trailSenseSection(key = "actions", title = toolsTitle) {
            row { position ->
                SkyValueRow(
                    title = stringResource(R.string.pref_sensor_details_title),
                    subtitle = stringResource(R.string.diagnostics_sensor_details_summary),
                    icon = Icons.Default.Sensors,
                    accent = TrailSenseAccents.Sky,
                    badgeShape = MaterialShapes.Cookie9Sided.toShape(),
                    position = position,
                    onClick = onShowSensorDetails,
                    modifier = Modifier.testTag("diagnostics_title"),
                    trailing = { DiagnosticsChevron() }
                )
            }
            row { position ->
                SkyValueRow(
                    title = stringResource(R.string.export_logs),
                    subtitle = stringResource(R.string.diagnostics_export_logs_summary),
                    icon = Icons.Default.Download,
                    accent = TrailSenseAccents.Lavender,
                    badgeShape = MaterialShapes.Clover4Leaf.toShape(),
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
            title = "${stringResource(title)} · ${items.size}",
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
private fun DiagnosticsHero(loaded: Boolean, errorCount: Int, warningCount: Int, checks: Int, reported: Int) {
    val issueCount = errorCount + warningCount
    val tone = when {
        errorCount > 0 -> TrailSenseTone.Error
        warningCount > 0 -> TrailSenseTone.Caution
        loaded -> TrailSenseTone.Good
        else -> TrailSenseTone.Secondary
    }
    val colors = trailSenseToneColors(tone)
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
    val progress by animateFloatAsState(
        if (checks == 0) 1f else (reported.toFloat() / checks).coerceIn(0f, 1f),
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "diagnosticsProgress"
    )
    // While the checks run the badge turns slowly; once they are in it settles.
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "diagnosticsSpin")
    val turning by spin.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(8_000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "diagnosticsTurn"
    )
    val status = trailSenseStatusColors()
    TrailSenseSpotlight(
        title = title,
        supportingText = supporting,
        tone = tone,
        titleStyle = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .then(if (issueCount == 0 && loaded) Modifier.testTag("empty_text") else Modifier),
        badge = {
            // The health ring: fills as the checks report, around a badge whose shape says the state.
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                if (loaded) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(96.dp),
                        color = colors.accent,
                        trackColor = colors.content.copy(alpha = 0.12f),
                        strokeWidth = 8.dp
                    )
                } else {
                    CircularWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(96.dp),
                        color = colors.accent,
                        trackColor = colors.content.copy(alpha = 0.12f)
                    )
                }
                TrailSenseShapeBadge(
                    icon = icon,
                    shape = when (tone) {
                        TrailSenseTone.Error -> MaterialShapes.SoftBurst.toShape()
                        TrailSenseTone.Caution -> MaterialShapes.Pentagon.toShape()
                        TrailSenseTone.Good -> MaterialShapes.Cookie9Sided.toShape()
                        else -> MaterialShapes.Cookie6Sided.toShape()
                    },
                    size = 64.dp,
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                    modifier = Modifier.graphicsLayer { rotationZ = if (loaded) 0f else turning }
                )
            }
        }
    ) {
        // Severity in one place: a proportional bar of errors, warnings and passing checks, with its legend.
        val passing = (checks - issueCount).coerceAtLeast(0)
        val segments = listOf(
            errorCount to MaterialTheme.colorScheme.error,
            warningCount to status.caution,
            passing to status.good
        ).filter { it.first > 0 }
        if (loaded && segments.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                segments.forEach { (count, color) ->
                    Box(
                        Modifier
                            .weight(count.toFloat())
                            .fillMaxHeight()
                            .background(color, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Text(
                listOfNotNull(
                    errorCount.takeIf { it > 0 }?.let { "$it ${stringResource(R.string.errors)}" },
                    warningCount.takeIf { it > 0 }?.let { "$it ${stringResource(R.string.diagnostics_warnings)}" },
                    stringResource(R.string.sky_diagnostics_checks_count, reported.coerceAtMost(checks), checks)
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = colors.content.copy(alpha = 0.82f)
            )
        }
    }
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

    // The row itself carries a breath of its severity, so red and amber read beyond the small icon.
    Surface(
        color = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            statusContainerColor(severity),
            0.32f
        ),
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
                shape = if (severity == ToolDiagnosticSeverity.Error) {
                    MaterialShapes.SoftBurst.toShape()
                } else {
                    MaterialShapes.Pentagon.toShape()
                },
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
        ToolDiagnosticSeverity.Warning -> trailSenseStatusColors().cautionContainer
    }
}

@Composable
private fun onStatusContainerColor(severity: ToolDiagnosticSeverity): Color {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> MaterialTheme.colorScheme.onErrorContainer
        ToolDiagnosticSeverity.Warning -> trailSenseStatusColors().onCautionContainer
    }
}

private fun statusIcon(severity: ToolDiagnosticSeverity): ImageVector {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> Icons.Default.ErrorOutline
        ToolDiagnosticSeverity.Warning -> Icons.Default.WarningAmber
    }
}
