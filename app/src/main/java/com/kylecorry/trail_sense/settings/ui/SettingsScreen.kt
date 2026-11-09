@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.main.MainActivity

import androidx.activity.compose.LocalActivity

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.material3.toShape

import androidx.compose.ui.graphics.Color
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTile
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTileGrid
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.icons.trail.Alert
import com.kylecorry.trail_sense.shared.icons.trail.Apps
import com.kylecorry.trail_sense.shared.icons.trail.BackupRestore
import com.kylecorry.trail_sense.shared.icons.trail.Experimental
import com.kylecorry.trail_sense.shared.icons.trail.Ruler
import com.kylecorry.trail_sense.shared.icons.trail.Sensors
import com.kylecorry.trail_sense.shared.icons.trail.Theme
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Visible
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort

private val navigationMap = mapOf(
    R.string.pref_unit_settings to Route.UnitSettings,
    R.string.pref_privacy_settings to Route.PrivacySettings,
    R.string.pref_experimental_settings to Route.ExperimentalSettings,
    R.string.pref_plugins_settings to Route.PluginsSettings,
    R.string.pref_error_settings to Route.ErrorSettings,
    R.string.pref_sensor_settings to Route.SensorSettings,
    R.string.pref_backup_settings to Route.BackupSettings,
    R.string.pref_tool_settings_header_key to Route.ToolsSettings,
    R.string.pref_open_source_licenses to Route.Licenses,
    R.string.pref_diagnostics to Route.Diagnostics
)

@Composable
fun SettingsToolScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    // The theme and the bottom navigation belong to the activity, so these switches reach it directly.
    val mainActivity = LocalActivity.current as MainActivity
    val plugins = remember { getAppService<PluginSubsystem>() }

    SettingsScreen(
        version = Package.getVersionName(context),
        pluginsEnabled = plugins.arePluginsEnabled(),
        onBack = rememberGoBack(),
        onNavigate = { navigator.navigate(it) },
        appearance = {
            val prefs = remember { com.kylecorry.trail_sense.shared.UserPreferences(context) }
            val cache = remember {
                com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem.getInstance(context).preferences
            }
            val fullscreenKey = stringResource(R.string.pref_night_mode_fullscreen)
            val compassColorsKey = stringResource(R.string.pref_use_dynamic_colors_on_compass)
            ThemeAppearanceControls(
                prefs = prefs,
                wallpaperAvailable = remember { com.google.android.material.color.DynamicColors.isDynamicColorAvailable() },
                nightFullscreen = remember { cache.getBoolean(fullscreenKey) ?: false },
                compassWallpaperColors = remember { cache.getBoolean(compassColorsKey) ?: false },
                onThemeChanged = {
                    prefs.theme = it
                    mainActivity.reloadTheme()
                },
                onPaletteChanged = { com.kylecorry.trail_sense.main.theme.ThemeChoices.changed() },
                onNightFullscreenChanged = {
                    cache.putBoolean(fullscreenKey, it)
                    mainActivity.reloadTheme()
                },
                onCompassWallpaperColorsChanged = { cache.putBoolean(compassColorsKey, it) }
            )
        },
        onCompactModeChanged = { mainActivity.changeBottomNavLabelsVisibility(it) },
        onShowAllLabelsChanged = { mainActivity.showAllBottomNavLabels(it) }
    )
}

/**
 * Settings, a calm list: appearance in place, the navigation bar's switches, the app's own pages (units first, with
 * its value), every tool that has settings, then the advanced pages and about. One quiet icon well leads every row so the groups line up.
 */
@Composable
private fun SettingsScreen(
    version: String,
    pluginsEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (Route) -> Unit,
    appearance: @Composable () -> Unit = {},
    onCompactModeChanged: (Boolean) -> Unit = {},
    onShowAllLabelsChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val toolGroups = remember {
        val categories = com.kylecorry.trail_sense.tools.tools.ui.sort.CategoricalToolSort(context)
            .sort(Tools.getTools(context).filter { it.settingsRoute != null })
            .filter { it.tools.isNotEmpty() }
        groupSettingsTools(categories.map { SettingsToolGroup(it.categoryName, it.tools) }, context.getString(R.string.other))
    }
    // Live values for the quick controls, read again whenever the screen comes back from a sub-page.
    var quick by remember { mutableStateOf(readQuickSettings(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        quick = readQuickSettings(context)
        onPauseOrDispose { }
    }

    val generalTitle = stringResource(R.string.general)
    val navigationBarTitle = stringResource(R.string.navigation_bar)
    val advancedTitle = stringResource(R.string.advanced)
    val aboutTitle = stringResource(R.string.pref_category_about)

    fun go(key: Int) = onNavigate(navigationMap.getValue(key))

    TrailSensePage(
        title = stringResource(R.string.settings),
        onBack = onBack,
        backLabel = stringResource(R.string.back)
    ) {
        // One question: where do I change this? The list answers it; the two settings people change most lead it,
        // with their live values.
        // Appearance applies in place, like ChangeDetection: no sub-page for the choices people make most.
        // Every appearance choice, including the rarer switches, lives here: there is no separate Theme page.
        item("appearance") {
            Column {
                TrailSenseGroupHeader(stringResource(R.string.appearance))
                appearance()
            }
        }
        // (No navigation bar section: the bar is the family's fixed four tabs, nothing to tune.)
        trailSenseSection("general", title = generalTitle) {
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.units),
                    icon = rememberVectorPainter(TrailIcons.Ruler),
                    value = quick.units,
                    position = position,
                    onClick = { go(R.string.pref_unit_settings) }
                )
            }
            // Quick actions and widgets apply across tools; a one-row "Tools" group above the tools would repeat
            // its own title.
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.sky_settings_tools_summary),
                    icon = rememberVectorPainter(Icons.Rounded.Widgets),
                    position = position,
                    onClick = { go(R.string.pref_tool_settings_header_key) }
                )
            }
            listOf(
                Triple(R.string.pref_sensor_settings, R.string.sensors, R.string.sky_settings_sensors_summary) to TrailIcons.Sensors,
                Triple(R.string.pref_privacy_settings, R.string.privacy, R.string.sky_settings_privacy_summary) to TrailIcons.Visible,
                Triple(R.string.pref_error_settings, R.string.errors, R.string.sky_settings_errors_summary) to TrailIcons.Alert,
                Triple(R.string.pref_backup_settings, R.string.backup_restore, R.string.sky_settings_backup_summary) to TrailIcons.BackupRestore
            ).forEach { (keys, icon) ->
                row { position ->
                    SettingsNavRow(
                        title = stringResource(keys.second),
                        subtitle = stringResource(keys.third),
                        icon = rememberVectorPainter(icon),
                        position = position,
                        onClick = { go(keys.first) }
                    )
                }
            }
        }

        // Each tool's own page grouped by its family, one row each; families with one tool share an "Other" group
        // rather than each getting a header for a single row.
        toolGroups.forEachIndexed { index, group ->
            trailSenseSection("tools_$index", title = group.title) {
                group.tools.forEach { tool ->
                    row { position ->
                        SettingsNavRow(
                            title = tool.name,
                            icon = rememberVectorPainter(tool.icon),
                            position = position,
                            onClick = { tool.settingsRoute?.let(onNavigate) }
                        )
                    }
                }
            }
        }

        trailSenseSection("advanced", title = advancedTitle) {
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.diagnostics),
                    icon = rememberVectorPainter(Icons.Rounded.MonitorHeart),
                    position = position,
                    onClick = { go(R.string.pref_diagnostics) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.experimental),
                    icon = rememberVectorPainter(TrailIcons.Experimental),
                    position = position,
                    onClick = { go(R.string.pref_experimental_settings) }
                )
            }
            if (pluginsEnabled) {
                row { position ->
                    SettingsNavRow(
                        title = stringResource(R.string.plugins),
                        icon = rememberVectorPainter(TrailIcons.Apps),
                        position = position,
                        onClick = { go(R.string.pref_plugins_settings) }
                    )
                }
            }
        }

        // About is one row, like WiFi Weakspot's: its page holds the version, the code, feedback and privacy once each.
        trailSenseSection("about", title = aboutTitle) {
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.about_app_title),
                    subtitle = stringResource(R.string.about_version, version),
                    icon = rememberVectorPainter(Icons.Rounded.Info),
                    position = position,
                    onClick = { onNavigate(Route.About) }
                )
            }
        }
    }
}

/**
 * One row of the Settings list: a 40 dp icon well, the title (and a one-line summary only when it adds something),
 * and a quiet trailing [value] when it has one. 64 dp tall on one line.
 */
@Composable
private fun SettingsNavRow(
    title: String,
    icon: Painter,
    position: TrailSenseGroupPosition,
    onClick: (() -> Unit)?,
    subtitle: String? = null,
    value: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = TrailSenseAnimation.quickSpring,
        label = "settingsRowPress"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            role = Role.Button,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    start = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    end = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WiFi Weakspot's icon well, the same on every settings row.
            SettingsRowIcon(painter = icon)
            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // No chevrons: every row of this list opens a page, so a chevron on each would only repeat that.
            if (value != null) {
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.merge(TextStyle(fontFeatureSettings = "tnum")),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(end = TrailSenseSpacing.sm)
                )
            }
        }
    }
}

private data class QuickSettings(val units: String, val compact: Boolean, val allLabels: Boolean)

private fun readQuickSettings(context: android.content.Context): QuickSettings {
    val prefs = com.kylecorry.trail_sense.shared.UserPreferences(context)
    val units = context.getString(
        if (prefs.distanceUnits == com.kylecorry.trail_sense.shared.UserPreferences.DistanceUnits.Meters) {
            R.string.metric
        } else {
            R.string.sky_units_imperial
        }
    )
    return QuickSettings(units, prefs.useCompactMode, prefs.useShowAllBottomNavigationLabels)
}

private data class SettingsToolGroup(
    val title: String?,
    val tools: List<com.kylecorry.trail_sense.tools.tools.infrastructure.Tool>
)

/** Families with a single tool go into one trailing [otherTitle] group (or join the last group if only one is left). */
private fun groupSettingsTools(groups: List<SettingsToolGroup>, otherTitle: String): List<SettingsToolGroup> {
    val (multi, single) = groups.partition { it.tools.size > 1 }
    val loose = single.flatMap { it.tools }.sortedBy { it.name }
    return when {
        loose.isEmpty() -> multi
        loose.size == 1 && multi.isNotEmpty() -> multi.dropLast(1) + multi.last().copy(tools = multi.last().tools + loose)
        else -> multi + SettingsToolGroup(otherTitle, loose)
    }
}
