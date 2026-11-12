package com.kylecorry.trail_sense.settings.backup

import kotlin.time.Clock

import android.content.Context
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.snackbar
import com.kylecorry.trail_sense.shared.dialogs.withLoading

class BackupCommand(private val context: Context, private val uriPicker: UriPicker) :
    CoroutineCommand {

    private val backupService = BackupService(context)

    override suspend fun execute() {
        // Alert the user before continuing
        val isCancelled = CoroutineAlerts.dialog(
            context,
            context.getString(R.string.backup),
            context.getString(R.string.backup_disclaimer),
            okText = context.getString(R.string.backup)
        )

        if (isCancelled) return

        // Select the destination file
        val destination = uriPicker.create(
            "trail-sense-${Clock.System.now().epochSeconds}.zip",
            "application/zip"
        ) ?: return

        Alerts.withLoading(
            context,
            context.getString(R.string.backing_up_loading_message)
        ) {
            backupService.backup(destination)
        }

        Alerts.snackbar(context, context.getString(R.string.done))
    }
}
