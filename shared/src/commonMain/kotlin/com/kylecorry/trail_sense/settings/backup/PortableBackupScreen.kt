package com.kylecorry.trail_sense.settings.backup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.sharing.pickFile
import com.kylecorry.trail_sense.shared.sharing.shareFile
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.*
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PortableBackupScreen(backup: PortableBackup) {
    val effects = LocalScreenEffects.current
    val strings = remember { getAppService<AppStrings>() }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    fun perform(error: String, operation: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                getAppService<com.kylecorry.trail_sense.shared.logging.Logger>()
                    .error("PortableBackup", error, exception)
                effects.showMessage(error)
            } finally {
                busy = false
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(appString(Res.string.restore)) },
            text = { Text(appString(Res.string.portable_backup_restore_description)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    perform(strings.get(Res.string.invalid_backup_file)) {
                        val archive = pickFile(listOf("zip")) ?: return@perform
                        val service = getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK)
                        if (service?.isEnabled() == true) {
                            effects.showMessage(strings.get(Res.string.portable_backup_stop_backtrack))
                            return@perform
                        }
                        backup.restore(archive.bytes)
                        getAppService<IPathService>().endBacktrackPath()
                        effects.showMessage(strings.get(Res.string.portable_backup_restored))
                    }
                }) { Text(appString(Res.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(appString(Res.string.back)) }
            }
        )
    }

    val backupLabel = appString(Res.string.backup)
    val restoreLabel = appString(Res.string.restore)
    TrailSensePage(
        title = appString(Res.string.portable_backup),
        onBack = rememberTrailSenseBack()
    ) {
        item { Text(appString(Res.string.portable_backup_description)) }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        trailSenseSection("portableBackup") {
            row { position ->
                TrailSenseListItem(
                    title = backupLabel,
                    position = position,
                    onClick = if (busy) null else {
                        {
                            perform(strings.get(Res.string.backup_failed)) {
                                check(shareFile("Trail Sense Backup.zip", backup.export()))
                            }
                        }
                    }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = restoreLabel,
                    position = position,
                    onClick = if (busy) null else { { confirmRestore = true } }
                )
            }
        }
    }
}
