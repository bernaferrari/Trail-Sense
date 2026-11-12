package com.kylecorry.trail_sense.settings.backup

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class BackupPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

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

    /** Where automatic backups go, as a platform location string (a document tree URI on Android). */
    var autoBackupLocation: String?
        get() = cache.getString(PreferenceKeys.autoBackupUri)?.takeIf { it.isNotBlank() }
        set(value) = cache.putString(PreferenceKeys.autoBackupUri, value ?: "")

    val autoBackupCount = 2

}
