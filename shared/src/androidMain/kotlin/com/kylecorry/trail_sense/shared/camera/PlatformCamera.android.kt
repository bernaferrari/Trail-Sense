package com.kylecorry.trail_sense.shared.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.camera.CameraSurface
import com.kylecorry.andromeda.camera.PhotoCamera
import com.kylecorry.andromeda.permissions.PermissionStatus
import com.kylecorry.andromeda.permissions.compose.rememberPermissionAccess
import com.kylecorry.trail_sense.shared.sharing.platformFileUri
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.kylecorry.andromeda.permissions.AppPermission as AndromedaPermission

@Composable
actual fun rememberSharedPermissionRequester(): suspend (AppPermission) -> Boolean {
    val access = rememberPermissionAccess()
    return remember(access) {
        { permission ->
            val mapped = permission.toAndromedaPermission()
            if (mapped == null) {
                true
            } else {
                val current = access.status(mapped)
                if (current == PermissionStatus.Granted) {
                    true
                } else {
                    access.request(mapped) == PermissionStatus.Granted
                }
            }
        }
    }
}

private fun AppPermission.toAndromedaPermission(): AndromedaPermission? = when (this) {
    AppPermission.Camera -> AndromedaPermission.Camera
    AppPermission.ActivityRecognition -> AndromedaPermission.Motion
    AppPermission.Backtrack -> AndromedaPermission.Location
    AppPermission.BackgroundLocation -> AndromedaPermission.BackgroundLocation
    AppPermission.ExactAlarms,
    AppPermission.IgnoreBatteryOptimizations -> null
}

@Composable
actual fun SharedCameraPreview(
    modifier: Modifier,
    onReady: (SharedCameraController?) -> Unit,
    onError: (Throwable) -> Unit,
    frameAnalysis: Boolean
) {
    CameraSurface(
        modifier = modifier,
        onReady = { camera -> onReady(camera?.let(::AndroidPhotoCameraAdapter)) },
        onError = onError,
        frameAnalysis = frameAnalysis
    )
}

private class AndroidPhotoCameraAdapter(
    private val delegate: PhotoCamera
) : SharedCameraController {
    override val frames: Flow<SharedCameraFrame> = delegate.frames.map { frame ->
        SharedCameraFrame(
            width = frame.width,
            height = frame.height,
            argb = frame.argb,
            rotationDegrees = frame.rotationDegrees
        )
    }

    override suspend fun capture(): ByteArray = delegate.capture()

    override fun setTorch(enabled: Boolean) {
        delegate.setTorch(enabled)
    }

    override fun flip() {
        delegate.flip()
    }

    override fun setZoom(ratio: Float) {
        delegate.setZoom(ratio)
    }
}

actual suspend fun saveCapturedPhotoToCache(bytes: ByteArray, prefix: String): String? {
    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
    val file = PlatformFile(FileKit.cacheDir, fileName)
    file.write(bytes)
    return platformFileUri(file)
}
