@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
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

@Composable
private fun SettingsScreen(
    version: String,
    pluginsEnabled: Boolean,
    onNavigate: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onEmailDeveloper: () -> Unit
) {
    val context = LocalContext.current
    val generalItems = listOfNotNull(
        SettingsItem(
            title = stringResource(R.string.units),
            subtitle = null,
            icon = R.drawable.ruler,
            navId = navigationMap.getValue(R.string.pref_unit_settings)
        ),
        SettingsItem(
            title = stringResource(R.string.sensors),
            subtitle = stringResource(R.string.sensor_summary),
            icon = R.drawable.ic_sensors,
            navId = navigationMap.getValue(R.string.pref_sensor_settings)
        ),
        SettingsItem(
            title = stringResource(R.string.pref_theme_title),
            subtitle = null,
            icon = R.drawable.ic_theme,
            navId = navigationMap.getValue(R.string.pref_theme_settings)
        ),
        SettingsItem(
            title = stringResource(R.string.privacy),
            subtitle = null,
            icon = R.drawable.ic_visible,
            navId = navigationMap.getValue(R.string.pref_privacy_settings)
        ),
        SettingsItem(
            title = stringResource(R.string.errors),
            subtitle = null,
            icon = R.drawable.ic_alert,
            navId = navigationMap.getValue(R.string.pref_error_settings)
        ),
        SettingsItem(
            title = stringResource(R.string.experimental),
            subtitle = null,
            icon = R.drawable.ic_experimental,
            navId = navigationMap.getValue(R.string.pref_experimental_settings)
        ),
        if (pluginsEnabled) {
            SettingsItem(
                title = stringResource(R.string.plugins),
                subtitle = null,
                icon = R.drawable.apps,
                navId = navigationMap.getValue(R.string.pref_plugins_settings)
            )
        } else {
            null
        },
        SettingsItem(
            title = stringResource(R.string.backup_restore),
            subtitle = null,
            icon = R.drawable.backup_restore,
            navId = navigationMap.getValue(R.string.pref_backup_settings)
        )
    )

    val toolItems = remember {
        AlphabeticalToolSort()
            .sort(Tools.getTools(context))
            .first()
            .tools
            .filter { it.settingsNavAction != null }
    }

    val allToolsItem = SettingsItem(
        title = stringResource(R.string.tools),
        subtitle = null,
        icon = R.drawable.apps,
        navId = navigationMap.getValue(R.string.pref_tool_settings_header_key)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
            item("title") {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        top = TrailSenseSpacing.sm,
                        bottom = TrailSenseSpacing.xs
                    )
                )
            }

            item("general_section") {
                SettingsSection(title = stringResource(R.string.general)) {
                    generalItems.forEachIndexed { index, item ->
                        SettingsCardRow(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon?.let { painterResource(it) },
                            position = TrailSenseGroupPosition.at(index, generalItems.size),
                            onClick = { onNavigate(item.navId) }
                        )
                    }
                }
            }

            item("tools_section") {
                SettingsSection(title = stringResource(R.string.tools)) {
                    SettingsCardRow(
                        title = allToolsItem.title,
                        subtitle = allToolsItem.subtitle,
                        icon = allToolsItem.icon?.let { painterResource(it) },
                        position = TrailSenseGroupPosition.at(0, toolItems.size + 1),
                        onClick = { onNavigate(allToolsItem.navId) }
                    )
                    toolItems.forEachIndexed { index, tool ->
                        SettingsCardRow(
                            title = tool.name,
                            subtitle = null,
                            icon = painterResource(tool.icon),
                            position = TrailSenseGroupPosition.at(index + 1, toolItems.size + 1),
                            onClick = { tool.settingsNavAction?.let(onNavigate) }
                        )
                    }
                }
            }

            item("about_section") {
                SettingsSection(title = stringResource(R.string.pref_category_about)) {
                    SettingsCardRow(
                        title = stringResource(R.string.diagnostics),
                        subtitle = null,
                        icon = null,
                        position = TrailSenseGroupPosition.at(0, 6),
                        onClick = { onNavigate(navigationMap.getValue(R.string.pref_diagnostics)) }
                    )
                    SettingsCardRow(
                        title = stringResource(R.string.pref_app_version_title),
                        subtitle = version,
                        icon = null,
                        position = TrailSenseGroupPosition.at(1, 6),
                        onClick = null
                    )
                    SettingsCardRow(
                        title = stringResource(R.string.pref_privacy_policy_title),
                        subtitle = stringResource(R.string.privacy_policy_url),
                        icon = null,
                        position = TrailSenseGroupPosition.at(2, 6),
                        onClick = { onOpenUrl(context.getString(R.string.privacy_policy_url)) }
                    )
                    SettingsCardRow(
                        title = stringResource(R.string.pref_email_title),
                        subtitle = stringResource(R.string.email),
                        icon = null,
                        position = TrailSenseGroupPosition.at(3, 6),
                        onClick = onEmailDeveloper
                    )
                    SettingsCardRow(
                        title = stringResource(R.string.pref_github_title),
                        subtitle = GITHUB_URL,
                        icon = null,
                        position = TrailSenseGroupPosition.at(4, 6),
                        onClick = { onOpenUrl(GITHUB_URL) }
                    )
                    SettingsCardRow(
                        title = stringResource(R.string.pref_open_source_licenses_title),
                        subtitle = null,
                        icon = null,
                        position = TrailSenseGroupPosition.at(5, 6),
                        onClick = {
                            onNavigate(navigationMap.getValue(R.string.pref_open_source_licenses))
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.lg
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            content()
        }
    }
}

@Composable
private fun SettingsCardRow(
    title: String,
    subtitle: String?,
    icon: Painter?,
    position: TrailSenseGroupPosition,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
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
            if (icon != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier.size(TrailSenseSize.leadingIcon)
                        )
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.md))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
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

            if (onClick != null) {
                Spacer(Modifier.width(TrailSenseSpacing.sm))
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

private data class SettingsItem(
    val title: String,
    val subtitle: String?,
    val icon: Int?,
    val navId: Int
)

private const val GITHUB_URL = "https://github.com/kylecorry31/Trail-Sense"
