package com.kylecorry.trail_sense.shared.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import kotlinx.coroutines.flow.Flow

data class SharedCameraFrame(
    val width: Int,
    val height: Int,
    val argb: IntArray,
    val rotationDegrees: Int? = null
)

interface SharedCameraController {
    val frames: Flow<SharedCameraFrame>
    suspend fun capture(): ByteArray
    fun setTorch(enabled: Boolean)
    fun flip()
    fun setZoom(ratio: Float)
}

@Composable
expect fun rememberSharedPermissionRequester(): suspend (AppPermission) -> Boolean

@Composable
expect fun SharedCameraPreview(
    modifier: Modifier = Modifier,
    onReady: (SharedCameraController?) -> Unit,
    onError: (Throwable) -> Unit = {},
    frameAnalysis: Boolean = false
)

expect suspend fun saveCapturedPhotoToCache(bytes: ByteArray, prefix: String = "photo"): String?
