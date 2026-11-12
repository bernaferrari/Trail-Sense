package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class CameraPreferences(context: Context) : PreferenceRepo(context), ICameraPreferences {

    override val useZeroShutterLag by BooleanPreference(
        cache,
        PreferenceKeys.useZeroShutterLag,
        false
    )

    override var projectionType by StringEnumPreference(
        cache,
        PreferenceKeys.augmentedRealityMapper,
        ProjectionType.entries.associateBy { it.id },
        ProjectionType.EstimatedIntrinsics
    )

    enum class ProjectionType(val id: String) {
        EstimatedIntrinsics("estimated_intrinsics"),
        ManufacturerIntrinsics("manufacturer_intrinsics"),
        Perspective("perspective"),
        Linear("linear")
    }

}
