package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.preferences.getLongArray
import com.kylecorry.andromeda.preferences.putLongArray
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

enum class FieldGuideNameDisplay(override val id: Long) : Identifiable {
    Common(1),
    Scientific(2),
    Both(3)
}

class FieldGuidePreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var hiddenPageIds: List<Long>
        get() = cache.getLongArray(PreferenceKeys.fieldGuideHiddenPages) ?: emptyList()
        set(value) {
            cache.putLongArray(PreferenceKeys.fieldGuideHiddenPages, value)
        }

    var nameDisplay by StringEnumPreference(
        cache,
        PreferenceKeys.fieldGuideNameDisplay,
        FieldGuideNameDisplay.entries.associateBy { it.id.toString() },
        FieldGuideNameDisplay.Common
    )
}
