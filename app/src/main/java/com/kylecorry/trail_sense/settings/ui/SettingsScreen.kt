@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Policy
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
import com.kylecorry.andromeda.core.system.Intents
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
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort

private val navigationMap = mapOf(
    R.string.pref_unit_settings to R.id.action_settings_to_unit_settings,
    R.string.pref_privacy_settings to R.id.action_settings_to_privacy_settings,
    R.string.pref_theme_settings to R.id.action_settings_to_theme_settings,
    R.string.pref_experimental_settings to R.id.action_settings_to_experimental_settings,
    R.string.pref_plugins_settings to R.id.action_settings_to_plugins_settings,
    R.string.pref_error_settings to R.id.action_settings_to_error_settings,
    R.string.pref_sensor_settings to R.id.action_settings_to_sensor_settings,
    R.string.pref_backup_settings to R.id.action_settings_to_backup_settings,
    R.string.pref_tool_settings_header_key to R.id.toolsSettingsFragment,
    R.string.pref_open_source_licenses to R.id.action_action_settings_to_licenseFragment,
    R.string.pref_diagnostics to R.id.action_settings_to_diagnostics
)

@Composable
fun SettingsToolScreen(host: TrailSenseDestinationHost) {
    val plugins = remember { getAppService<PluginSubsystem>() }

    SettingsScreen(
        version = Package.getVersionName(host.requireContext()),
        pluginsEnabled = plugins.arePluginsEnabled(),
        onBack = { host.mainActivity.onBackPressedDispatcher.onBackPressed() },
        onNavigate = { host.findNavController().navigateWithAnimation(it) },
        onOpenUrl = { host.startActivity(Intents.url(it)) },
        onEmailDeveloper = {
            val intent = Intents.email(
                host.getString(R.string.email),
                host.getString(R.string.app_name)
            )
            host.startActivity(
                Intent.createChooser(
                    intent,
                    host.getString(R.string.pref_email_title)
                )
            )
        }
    )
}

/**
 * Settings: the app's own pages, then every tool that has settings, then the experiments and plugins, then about.
 * One line per row wherever the title says enough; an icon well leads every row so the groups line up.
 */
@Composable
private fun SettingsScreen(
    version: String,
    pluginsEnabled: Boolean,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onEmailDeveloper: () -> Unit
) {
    val context = LocalContext.current
    val toolItems = remember {
        AlphabeticalToolSort()
            .sort(Tools.getTools(context))
            .first()
            .tools
            .filter { it.settingsNavAction != null }
    }

    val generalTitle = stringResource(R.string.general)
    val toolsTitle = stringResource(R.string.tools)
    val advancedTitle = stringResource(R.string.advanced)
    val aboutTitle = stringResource(R.string.pref_category_about)

    fun go(key: Int) = onNavigate(navigationMap.getValue(key))

    TrailSensePage(
        title = stringResource(R.string.settings),
        onBack = onBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("general", title = generalTitle) {
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.units),
                    icon = rememberVectorPainter(TrailIcons.Ruler),
                    position = position,
                    onClick = { go(R.string.pref_unit_settings) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.pref_theme_title),
                    icon = rememberVectorPainter(TrailIcons.Theme),
                    position = position,
                    onClick = { go(R.string.pref_theme_settings) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.sensors),
                    subtitle = stringResource(R.string.sensor_summary),
                    icon = rememberVectorPainter(TrailIcons.Sensors),
                    position = position,
                    onClick = { go(R.string.pref_sensor_settings) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.privacy),
                    icon = rememberVectorPainter(TrailIcons.Visible),
                    position = position,
                    onClick = { go(R.string.pref_privacy_settings) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.errors),
                    icon = rememberVectorPainter(TrailIcons.Alert),
                    position = position,
                    onClick = { go(R.string.pref_error_settings) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.backup_restore),
                    icon = rememberVectorPainter(TrailIcons.BackupRestore),
                    position = position,
                    onClick = { go(R.string.pref_backup_settings) }
                )
            }
        }

        trailSenseSection("tools", title = toolsTitle) {
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.tools),
                    subtitle = stringResource(R.string.settings_tools_summary),
                    icon = rememberVectorPainter(TrailIcons.Apps),
                    position = position,
                    onClick = { go(R.string.pref_tool_settings_header_key) }
                )
            }
            toolItems.forEach { tool ->
                row { position ->
                    SettingsNavRow(
                        title = tool.name,
                        icon = rememberVectorPainter(tool.icon),
                        position = position,
                        onClick = { tool.settingsNavAction?.let(onNavigate) }
                    )
                }
            }
        }

        trailSenseSection("advanced", title = advancedTitle) {
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

        trailSenseSection("about", title = aboutTitle) {
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
                    title = stringResource(R.string.pref_app_version_title),
                    icon = rememberVectorPainter(Icons.Rounded.Info),
                    position = position,
                    value = version,
                    onClick = null
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.pref_privacy_policy_title),
                    icon = rememberVectorPainter(Icons.Rounded.Policy),
                    position = position,
                    onClick = { onOpenUrl(context.getString(R.string.privacy_policy_url)) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.pref_email_title),
                    icon = rememberVectorPainter(Icons.Rounded.Email),
                    position = position,
                    onClick = onEmailDeveloper
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.pref_github_title),
                    icon = rememberVectorPainter(Icons.Rounded.Code),
                    position = position,
                    onClick = { onOpenUrl(GITHUB_URL) }
                )
            }
            row { position ->
                SettingsNavRow(
                    title = stringResource(R.string.pref_open_source_licenses_title),
                    icon = rememberVectorPainter(Icons.Rounded.Description),
                    position = position,
                    onClick = { go(R.string.pref_open_source_licenses) }
                )
            }
        }
    }
}

/**
 * One row of the Settings list: a 40 dp icon well, the title (and a summary only when it adds something), and a
 * chevron when it opens something, or a quiet [value] when it only reports one. 64 dp tall on one line.
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
        color = MaterialTheme.colorScheme.surfaceContainerLow
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
            Surface(
                shape = RoundedCornerShape(TrailSenseSpacing.md),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(TrailSenseSize.compactIconContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(TrailSenseSize.compactIcon)
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
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
            if (onClick != null) {
                Spacer(Modifier.width(TrailSenseSpacing.xs))
                Box(
                    modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private const val GITHUB_URL = "https://github.com/kylecorry31/Trail-Sense"
