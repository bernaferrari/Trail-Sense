package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.backup.BackupCommand
import com.kylecorry.trail_sense.settings.backup.ChangeAutomaticBackupDirectoryCommand
import com.kylecorry.trail_sense.settings.backup.RestoreCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.launch

class BackupSettingsFragment : Fragment() {

    private val resultRetriever by lazy { requireActivity() as IntentResultRetriever }
    private val uriPicker by lazy { IntentUriPicker(resultRetriever, requireContext()) }
    private val backupCommand by lazy { BackupCommand(requireContext(), uriPicker) }
    private val restoreCommand by lazy { RestoreCommand(requireContext(), uriPicker) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var autoBackupEnabled by remember {
                        mutableStateOf(prefs.backup.isAutoBackupEnabled)
                    }
                    var autoBackupSummary by remember {
                        mutableStateOf(getAutoBackupSummary(autoBackupEnabled))
                    }
                    var includeMapsforge by remember {
                        mutableStateOf(prefs.backup.includeMapsforgeMaps)
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
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.backup_restore)
                                )
                            }
                            item("backup") {
                                SettingsActionRow(
                                    label = stringResource(R.string.backup),
                                    onClick = ::backup,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("restore") {
                                SettingsActionRow(
                                    label = stringResource(R.string.restore),
                                    onClick = ::restore,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("auto_backup") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.automatic_backup),
                                    summary = autoBackupSummary,
                                    checked = autoBackupEnabled,
                                    onCheckedChange = {
                                        autoBackupEnabled = it
                                        prefs.backup.isAutoBackupEnabled = it
                                        if (it) {
                                            changeAutoBackupDirectory {
                                                autoBackupEnabled = it
                                                autoBackupSummary = getAutoBackupSummary(it)
                                            }
                                        } else {
                                            autoBackupSummary = null
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("mapsforge") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.include_mapsforge_backups),
                                    summary = stringResource(R.string.include_mapsforge_backups_summary),
                                    checked = includeMapsforge,
                                    onCheckedChange = {
                                        includeMapsforge = it
                                        prefs.backup.includeMapsforgeMaps = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun backup() {
        viewLifecycleOwner.lifecycleScope.launch {
            backupCommand.execute()
        }
    }

    private fun restore() {
        viewLifecycleOwner.lifecycleScope.launch {
            restoreCommand.execute()
        }
    }

    private fun changeAutoBackupDirectory(onChanged: (Boolean) -> Unit) {
        ChangeAutomaticBackupDirectoryCommand(requireContext(), resultRetriever) { isSuccessful ->
            if (!isSuccessful) {
                prefs.backup.isAutoBackupEnabled = false
            }
            onChanged(isSuccessful)
        }.execute()
    }

    private fun getAutoBackupSummary(isEnabled: Boolean): String? {
        return if (isEnabled) {
            prefs.backup.userFriendlyAutoBackupPathName
        } else {
            null
        }
    }
}
