package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class BackupPreferences(context: Context) : PreferenceRepo(context) {

    var isAutoBackupEnabled by BooleanPreference(
        cache,
        PreferenceKeys.autoBackupEnabled,
        false
    )

    var includeMapsforgeMaps by BooleanPreference(
        cache,
        PreferenceKeys.includeMapsforgeBackups,
        true
    )

    private var autoBackupUriString: String by StringPreference(
        cache,
        PreferenceKeys.autoBackupUri,
        ""
    )

    var autoBackupUri: Uri?
        get() {
            return if (autoBackupUriString.isBlank()) {
                null
            } else {
                autoBackupUriString.toUri()
            }
        }
        set(value) {
            autoBackupUriString = value?.toString() ?: ""
        }

    val userFriendlyAutoBackupPathName: String
        get() {
            val path = autoBackupUri?.path ?: return ""
            val colonIndex = path.indexOf(":")
            return if (colonIndex != -1) {
                path.substring(colonIndex + 1)
            } else {
                path
            }
        }

    val autoBackupCount = 2

}
