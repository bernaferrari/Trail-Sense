@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
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
fun SettingsToolScreen(fragment: Fragment) {
    val plugins = remember { getAppService<PluginSubsystem>() }

    SettingsScreen(
        version = Package.getVersionName(fragment.requireContext()),
        pluginsEnabled = plugins.arePluginsEnabled(),
        onNavigate = { fragment.findNavController().navigateWithAnimation(it) },
        onOpenUrl = { fragment.startActivity(Intents.url(it)) },
        onEmailDeveloper = {
            val intent = Intents.email(
                fragment.getString(R.string.email),
                fragment.getString(R.string.app_name)
            )
            fragment.startActivity(
                Intent.createChooser(
                    intent,
                    fragment.getString(R.string.pref_email_title)
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("general_header") {
                    TrailSenseSectionHeader(title = stringResource(R.string.general))
                }
                items(generalItems, key = { it.title }) { item ->
                    SettingsRow(
                        item = item,
                        onClick = { onNavigate(item.navId) }
                    )
                }

                item("tools_header") {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.tools),
                        modifier = Modifier.padding(top = TrailSenseSpacing.md)
                    )
                }
                item("all_tools_settings") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.tools),
                            subtitle = null,
                            icon = R.drawable.apps,
                            navId = navigationMap.getValue(R.string.pref_tool_settings_header_key)
                        ),
                        onClick = {
                            onNavigate(navigationMap.getValue(R.string.pref_tool_settings_header_key))
                        }
                    )
                }
                items(toolItems, key = { "tool_${it.id}" }) { tool ->
                    ToolSettingsRow(
                        tool = tool,
                        onClick = { tool.settingsNavAction?.let(onNavigate) }
                    )
                }

                item("about_header") {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.pref_category_about),
                        modifier = Modifier.padding(top = TrailSenseSpacing.md)
                    )
                }
                item("diagnostics") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.diagnostics),
                            subtitle = null,
                            icon = null,
                            navId = navigationMap.getValue(R.string.pref_diagnostics)
                        ),
                        onClick = { onNavigate(navigationMap.getValue(R.string.pref_diagnostics)) }
                    )
                }
                item("version") {
                    TrailSenseListItem(
                        title = stringResource(R.string.pref_app_version_title),
                        subtitle = version,
                        animateEntrance = false
                    )
                    TrailSenseDivider()
                }
                item("privacy_policy") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.pref_privacy_policy_title),
                            subtitle = stringResource(R.string.privacy_policy_url),
                            icon = null,
                            navId = 0
                        ),
                        onClick = { onOpenUrl(context.getString(R.string.privacy_policy_url)) }
                    )
                }
                item("email") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.pref_email_title),
                            subtitle = stringResource(R.string.email),
                            icon = null,
                            navId = 0
                        ),
                        onClick = onEmailDeveloper
                    )
                }
                item("github") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.pref_github_title),
                            subtitle = GITHUB_URL,
                            icon = null,
                            navId = 0
                        ),
                        onClick = { onOpenUrl(GITHUB_URL) }
                    )
                }
                item("licenses") {
                    SettingsRow(
                        item = SettingsItem(
                            title = stringResource(R.string.pref_open_source_licenses_title),
                            subtitle = null,
                            icon = null,
                            navId = navigationMap.getValue(R.string.pref_open_source_licenses)
                        ),
                        onClick = {
                            onNavigate(navigationMap.getValue(R.string.pref_open_source_licenses))
                        }
                    )
                }
            }
        }
    }

@Composable
private fun SettingsRow(
    item: SettingsItem,
    onClick: () -> Unit
) {
        TrailSenseListItem(
            title = item.title,
            subtitle = item.subtitle,
            icon = item.icon?.let { painterResource(it) },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onClick,
            animateEntrance = false
        )
        TrailSenseDivider()
}

@Composable
private fun ToolSettingsRow(
    tool: Tool,
    onClick: () -> Unit
) {
        TrailSenseListItem(
            title = tool.name,
            icon = painterResource(tool.icon),
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onClick,
            animateEntrance = false
        )
        TrailSenseDivider()
}

private data class SettingsItem(
    val title: String,
    val subtitle: String?,
    val icon: Int?,
    val navId: Int
)

private const val GITHUB_URL = "https://github.com/kylecorry31/Trail-Sense"
