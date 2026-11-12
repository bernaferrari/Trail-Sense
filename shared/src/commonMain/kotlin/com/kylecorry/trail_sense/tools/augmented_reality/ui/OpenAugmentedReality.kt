package com.kylecorry.trail_sense.tools.augmented_reality.ui

import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import kotlin.time.Instant

/** Opens the augmented reality tool (where the platform has one) in [mode]. */
fun openAugmentedReality(
    appNavigator: TrailSenseNavigator,
    mode: ARMode = ARMode.Normal,
    enableCamera: Boolean = true,
    time: Instant? = null
) {
    appNavigator.navigate(
        Route.AugmentedReality(modeId = mode.id, cameraEnabled = enableCamera, time = time?.toString())
    )
}
