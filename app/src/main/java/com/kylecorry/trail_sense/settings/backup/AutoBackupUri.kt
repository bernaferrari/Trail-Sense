package com.kylecorry.trail_sense.settings.backup

import android.net.Uri
import androidx.core.net.toUri

var BackupPreferences.autoBackupUri: Uri?
    get() = autoBackupLocation?.toUri()
    set(value) {
        autoBackupLocation = value?.toString()
    }

/** The backup folder as the user would recognize it, without the storage volume prefix. */
val BackupPreferences.userFriendlyAutoBackupPathName: String
    get() {
        val path = autoBackupUri?.path ?: return ""
        val colonIndex = path.indexOf(":")
        return if (colonIndex != -1) {
            path.substring(colonIndex + 1)
        } else {
            path
        }
    }
