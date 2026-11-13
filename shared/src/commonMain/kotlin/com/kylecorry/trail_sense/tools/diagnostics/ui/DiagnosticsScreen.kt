@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.diagnostics.ui




import androidx.compose.ui.semantics.stateDescription

import androidx.compose.ui.semantics.semantics

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import com.kylecorry.trail_sense.shared.text.appPluralString
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.Altitude
import com.kylecorry.trail_sense.shared.icons.trail.BackupRestore
import com.kylecorry.trail_sense.shared.icons.trail.Barometer
import com.kylecorry.trail_sense.shared.icons.trail.Camera
import com.kylecorry.trail_sense.shared.icons.trail.CompassIcon
import com.kylecorry.trail_sense.shared.icons.trail.Flashlight
import com.kylecorry.trail_sense.shared.icons.trail.Gyro
import com.kylecorry.trail_sense.shared.icons.trail.Notification
import com.kylecorry.trail_sense.shared.icons.trail.Satellite
import com.kylecorry.trail_sense.shared.icons.trail.Steps
import com.kylecorry.trail_sense.shared.icons.trail.Sunny
import com.kylecorry.trail_sense.shared.icons.trail.ToolBattery
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Weather
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.platform.LocalPlatformContext
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.compose.runtime.rememberCoroutineScope
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDescriptor
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.navigation.Route

/** How long to wait for every scanner to report before the screen stops saying it's still checking. */
private const val DIAGNOSTICS_SETTLE_MILLIS = 3000L

/**
 * Diagnostics: a hero with the one answer (all good, or how many things need attention), the issues grouped by
 * severity, then the two things you can do here (sensor details, export logs). The all-clear never shows before the
 * scanners have had a chance to report.
 */
@Composable
fun DiagnosticsScreen(
    onShowSensorDetails: () -> Unit,
    onShowDiagnostic: (DiagnosticItem) -> Unit,
    onExportLogs: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val dispatcher = rememberSystemBack()
    val tools = remember { getAppService<ToolCatalog>().getTools() }
    val diagnosticIdToTool = remember(tools) {
        val toolMap = mutableMapOf<String, List<ToolDescriptor>>()
        tools.forEach {
            it.diagnostics.forEach { diagnostic ->
                toolMap[diagnostic.id] = toolMap[diagnostic.id].orEmpty() + it
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

    // One question: is anything stopping a tool from working? The issue count answers it; each issue is one row.
    val toolsTitle = appString(Res.string.sky_diagnostics_tools)
    val errorsTitle = appPluralString(Res.plurals.diagnostics_error_count, errors.size, errors.size)
    val warningsTitle = appPluralString(Res.plurals.diagnostics_warning_count, warnings.size, warnings.size)
    TrailSensePage(
        title = appString(Res.string.diagnostics),
        onBack = { dispatcher?.invoke() },
        backLabel = appString(Res.string.back),
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

        diagnosticsSection(
            "errors",
            errorsTitle,
            errors,
            onShowDiagnostic
        )
        diagnosticsSection(
            "warnings",
            warningsTitle,
            warnings,
            onShowDiagnostic
        )

        trailSenseSection(key = "actions", title = toolsTitle) {
            row { position ->
                SkyValueRow(
                    title = appString(Res.string.pref_sensor_details_title),
                    subtitle = appString(Res.string.diagnostics_sensor_details_summary),
                    icon = Icons.Default.Sensors,
                    position = position,
                    onClick = onShowSensorDetails,
                    modifier = Modifier.testTag("diagnostics_title"),
                    trailing = { DiagnosticsChevron() }
                )
            }
            if (onExportLogs != null) {
                row { position ->
                    SkyValueRow(
                        title = appString(Res.string.export_logs),
                        subtitle = appString(Res.string.diagnostics_export_logs_summary),
                        icon = Icons.Default.Download,
                        position = position,
                        onClick = onExportLogs,
                        modifier = Modifier.testTag("download_logs")
                    )
                }
            }
        }
    }
}

private fun LazyListScope.diagnosticsSection(
    key: String,
    title: String,
    items: List<DiagnosticItem>,
    onShowDiagnostic: (DiagnosticItem) -> Unit
) {
    if (items.isEmpty()) return
    item(key = key) {
        TrailSenseRowGroup(
            title = title,
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
    val title = when {
        // The count is in the cookie, so the title is only the words.
        issueCount > 0 -> appPluralString(Res.plurals.diagnostics_issues_found, issueCount)
        loaded -> appString(Res.string.no_issues)
        else -> appString(Res.string.diagnostics_checking)
    }
    // One quiet line under the count only when there are none, saying why that is good news; the sections below
    // already split errors from warnings.
    val supporting = when {
        issueCount > 0 -> null
        loaded -> appString(Res.string.diagnostics_no_issues_summary)
        else -> null
    }
    val progress by animateFloatAsState(
        if (checks == 0) 1f else (reported.toFloat() / checks).coerceIn(0f, 1f),
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "diagnosticsProgress"
    )
    // The answer as WiFi Weakspot's signal card: a quiet surface where only the cookie wears the verdict's colour and
    // holds the count (or a check), and the words beside it. The title stays smaller than the page title above it.
    val tone = when {
        !loaded && issueCount == 0 -> TrailSenseTone.Neutral
        errorCount > 0 -> TrailSenseTone.Error
        warningCount > 0 -> TrailSenseTone.Caution
        else -> TrailSenseTone.Good
    }
    val colors = trailSenseToneColors(tone)
    TrailSenseSpotlight(
        title = title,
        supportingText = supporting,
        tone = TrailSenseTone.Neutral,
        titleStyle = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .then(if (issueCount == 0 && loaded) Modifier.testTag("empty_text") else Modifier),
        badge = {
            TrailSenseShapeBadge(
                text = if (issueCount > 0) issueCount.toString() else null,
                icon = when {
                    issueCount > 0 -> null
                    loaded -> Icons.Default.CheckCircle
                    else -> Icons.Default.HourglassEmpty
                },
                shape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        }
    ) {
        if (!loaded) {
            // While the checks run, the bar fills as they report: the motion carries the progress.
            androidx.compose.material3.LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
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
    val isError = severity == ToolDiagnosticSeverity.Error
    val severityLabel = appString(if (isError) Res.string.error else Res.string.warning)
    SkyValueRow(
        title = item.result.name,
        subtitle = item.result.description,
        // The glyph says what the issue is about (steps, GPS, alarms); only a real error gets the error container,
        // warnings sit in the family's quiet well.
        icon = issueIcon(item),
        iconContainerColor = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        },
        iconContentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.tertiary,
        position = position,
        onClick = onClick,
        modifier = Modifier.semantics {
            stateDescription = severityLabel
        },
        trailing = {
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            DiagnosticsChevron()
        }
    )
}

@Composable
private fun DiagnosticsChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** What the issue is about, from its id; the tool's own icon, then the severity mark, when nothing matches. */
private fun issueIcon(item: DiagnosticItem): ImageVector {
    val id = item.result.id.lowercase()
    return when {
        "pedometer" in id -> TrailIcons.Steps
        "background-location" in id || "location" in id || "gps" in id -> TrailIcons.Satellite
        "alarm" in id -> TrailIcons.Notification
        "notification" in id -> TrailIcons.Notification
        "weather" in id -> TrailIcons.Weather
        "battery" in id || "power" in id -> TrailIcons.ToolBattery
        "camera" in id -> TrailIcons.Camera
        "flashlight" in id -> TrailIcons.Flashlight
        "light-sensor" in id -> TrailIcons.Sunny
        "barometer" in id -> TrailIcons.Barometer
        "altitude" in id -> TrailIcons.Altitude
        "accelerometer" in id || "gyroscope" in id -> TrailIcons.Gyro
        "magnetometer" in id || "compass" in id -> TrailIcons.CompassIcon
        "backup" in id -> TrailIcons.BackupRestore
        else -> item.tools.firstOrNull()?.icon ?: statusIcon(item.result.severity)
    }
}

private fun statusIcon(severity: ToolDiagnosticSeverity): ImageVector {
    return when (severity) {
        ToolDiagnosticSeverity.Error -> Icons.Default.ErrorOutline
        ToolDiagnosticSeverity.Warning -> Icons.Default.WarningAmber
    }
}

/**
 * Diagnostics: what is wrong with the sensors and permissions, each with its fix. [exportLogs] saves the app's logs
 * to a file the person picks, on platforms that keep them; the row is hidden when null.
 */
@Composable
fun DiagnosticsToolScreen(exportLogs: (suspend () -> Unit)?) {
    val context = LocalPlatformContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    DiagnosticsScreen(
        onShowSensorDetails = {
            navigator.navigate(Route.SensorDetails)
        },
        onShowDiagnostic = {
            DiagnosticAlerter().alert(it) { action ->
                action.action(context, navigator)
            }
        },
        onExportLogs = exportLogs?.let { export -> { scope.launch { export() } } }
    )
}
