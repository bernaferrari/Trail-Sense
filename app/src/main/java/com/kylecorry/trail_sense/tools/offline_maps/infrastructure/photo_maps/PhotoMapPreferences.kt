package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.tools.offline_maps.domain.sort.MapSortMethod

class PhotoMapPreferences(context: Context) : PreferenceRepo(context) {
    var autoReducePhotoMaps by BooleanPreference(
        cache,
        PreferenceKeys.lowResolutionMaps,
        true
    )

    var autoReducePdfMaps by BooleanPreference(
        cache,
        PreferenceKeys.lowResolutionPdfMaps,
        true
    )

    var showMapPreviews by BooleanPreference(
        cache,
        PreferenceKeys.showMapPreviews,
        true
    )

    var mapSort: MapSortMethod by IntEnumPreference(
        cache,
        PreferenceKeys.mapSort,
        MapSortMethod.values().associateBy { it.id.toInt() },
        MapSortMethod.MostRecent
    )

    var keepMapFacingUp by BooleanPreference(
        cache,
        PreferenceKeys.keepMapFacingUp,
        true
    )

    var keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        PreferenceKeys.photoMapsKeepUnlocked,
        false
    )

    var highDetailMode by BooleanPreference(
        cache,
        PreferenceKeys.photoMapsHighDetailMode,
        true
    )

    val copyTrailMapsToAppStorage by BooleanPreference(
        cache,
        PreferenceKeys.copyTrailMapsToAppStorage,
        true
    )
}
