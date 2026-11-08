package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class CameraFlash { Off, On, Auto }

interface PhotoCamera {
    /** Captures an encoded image. */
    suspend fun capture(): ByteArray
    fun setZoom(ratio: Float)
    fun setTorch(enabled: Boolean)
    fun setFlash(mode: CameraFlash)
    fun flip()
}

/** Displays a camera preview. Camera permission must be granted before composition. */
@Composable
expect fun CameraSurface(
    modifier: Modifier = Modifier,
    onReady: (PhotoCamera?) -> Unit,
    onError: (Throwable) -> Unit = {},
)
