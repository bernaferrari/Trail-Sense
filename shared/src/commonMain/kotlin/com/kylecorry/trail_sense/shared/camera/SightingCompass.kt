package com.kylecorry.trail_sense.shared.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope

/** A camera sheet for sighting a bearing: the live compass bearing over the camera preview. */
interface SightingCompass {
    /** The compass bearing shown over the preview. */
    var bearing: Float?

    fun show()

    fun dismiss()

    /** Draws the sheet while it is shown. */
    @Composable
    fun Content()
}

/** Makes [SightingCompass] sheets; supplied by platforms with a camera. */
fun interface SightingCompassFactory {
    fun create(
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        onSelect: (bearing: Float?) -> Unit
    ): SightingCompass
}

/** The platform's sighting compass, or null where there is none (screens then hide camera sighting). */
val LocalSightingCompassFactory = staticCompositionLocalOf<SightingCompassFactory?> { null }

/** A sighting compass for this screen that reports the chosen bearing to [onSelect]; dismissed when it leaves. */
@Composable
fun rememberSightingCompass(onSelect: (bearing: Float?) -> Unit): SightingCompass? {
    val factory = LocalSightingCompassFactory.current ?: return null
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSelect by rememberUpdatedState(onSelect)
    val compass = remember(factory, lifecycleOwner) {
        factory.create(lifecycleOwner, lifecycleOwner.lifecycleScope) { latestOnSelect(it) }
    }
    DisposableEffect(compass) {
        onDispose { compass.dismiss() }
    }
    return compass
}
