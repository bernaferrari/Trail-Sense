package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsToolScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The activity implements the Activity-only IntentResultRetriever the file pickers need.
    val resultRetriever = context as IntentResultRetriever
    val prefs = remember { UserPreferences(context) }
    var autoBackupEnabled by remember {
        mutableStateOf(prefs.backup.isAutoBackupEnabled)
    }
    var autoBackupSummary by remember {
        mutableStateOf(getAutoBackupSummary(prefs, autoBackupEnabled))
    }
    var includeMapsforge by remember {
        mutableStateOf(prefs.backup.includeMapsforgeMaps)
    }

    SettingsList(title = stringResource(R.string.backup_restore)) {
        trailSenseSection("manual") {
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.backup),
                    onClick = { scope.launch { backup(context, resultRetriever) } },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.restore),
                    onClick = { scope.launch { restore(context, resultRetriever) } },
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
                            changeAutoBackupDirectory(context, resultRetriever, prefs) { isEnabled ->
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

private suspend fun backup(context: Context, resultRetriever: IntentResultRetriever) {
    BackupCommand(context, IntentUriPicker(resultRetriever, context)).execute()
}

private suspend fun restore(context: Context, resultRetriever: IntentResultRetriever) {
    RestoreCommand(context, IntentUriPicker(resultRetriever, context)).execute()
}

private fun changeAutoBackupDirectory(
    context: Context,
    resultRetriever: IntentResultRetriever,
    prefs: UserPreferences,
    onChanged: (Boolean) -> Unit
) {
    ChangeAutomaticBackupDirectoryCommand(context, resultRetriever) { isSuccessful ->
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
