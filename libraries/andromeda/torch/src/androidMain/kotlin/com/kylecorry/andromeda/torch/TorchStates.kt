package com.kylecorry.andromeda.torch

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.ksensor.core.context.KSensorContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

public actual fun torchStates(): Flow<TorchState> = callbackFlow {
    val context = KSensorContext.get()
    val cameraId = AndroidTorch(context).cameraId
    if (cameraId == null) {
        trySend(TorchState.Unavailable)
        close()
        return@callbackFlow
    }
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) trySend(if (enabled) TorchState.On else TorchState.Off)
        }
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) trySend(TorchState.Unavailable)
        }
    }
    manager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    awaitClose { manager.unregisterTorchCallback(callback) }
}.distinctUntilChanged()
