package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()) }
    var autoBackupEnabled by remember {
        mutableStateOf(prefs.backup.isAutoBackupEnabled)
    }
    var autoBackupSummary by remember {
        mutableStateOf(getAutoBackupSummary(prefs, autoBackupEnabled))
    }
    var includeMapsforge by remember {
        mutableStateOf(prefs.backup.includeMapsforgeMaps)
    }

    SettingsList(host, title = stringResource(R.string.backup_restore)) {
        trailSenseSection("manual") {
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.backup),
                    onClick = host::backup,
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.restore),
                    onClick = host::restore,
                    position = position
                )
            }
        }
        trailSenseSection("options") {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.automatic_backup),
                    summary = autoBackupSummary,
                    checked = autoBackupEnabled,
                    onCheckedChange = {
                        autoBackupEnabled = it
                        prefs.backup.isAutoBackupEnabled = it
                        if (it) {
                            host.changeAutoBackupDirectory(prefs) { isEnabled ->
                                autoBackupEnabled = isEnabled
                                autoBackupSummary = getAutoBackupSummary(prefs, isEnabled)
                            }
                        } else {
                            autoBackupSummary = null
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.include_mapsforge_backups),
                    summary = stringResource(R.string.include_mapsforge_backups_summary),
                    checked = includeMapsforge,
                    onCheckedChange = {
                        includeMapsforge = it
                        prefs.backup.includeMapsforgeMaps = it
                    },
                    position = position
                )
            }
        }
    }
}

private fun TrailSenseDestinationHost.backup() {
    val uriPicker = IntentUriPicker(mainActivity as IntentResultRetriever, requireContext())
    lifecycleScope.launch {
        BackupCommand(requireContext(), uriPicker).execute()
    }
}

private fun TrailSenseDestinationHost.restore() {
    val uriPicker = IntentUriPicker(mainActivity as IntentResultRetriever, requireContext())
    lifecycleScope.launch {
        RestoreCommand(requireContext(), uriPicker).execute()
    }
}

private fun TrailSenseDestinationHost.changeAutoBackupDirectory(
    prefs: UserPreferences,
    onChanged: (Boolean) -> Unit
) {
    val resultRetriever = mainActivity as IntentResultRetriever
    ChangeAutomaticBackupDirectoryCommand(requireContext(), resultRetriever) { isSuccessful ->
        if (!isSuccessful) {
            prefs.backup.isAutoBackupEnabled = false
        }
        onChanged(isSuccessful)
    }.execute()
}

private fun getAutoBackupSummary(prefs: UserPreferences, isEnabled: Boolean): String? {
    return if (isEnabled) {
        prefs.backup.userFriendlyAutoBackupPathName
    } else {
        null
    }
}
