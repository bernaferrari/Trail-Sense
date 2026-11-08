@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.torch

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.AVFoundation.*

actual fun torchStates(): Flow<TorchState> = flow {
    while (currentCoroutineContext().isActive) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        emit(when {
            device == null || !device.hasTorch || !device.torchAvailable -> TorchState.Unavailable
            device.torchActive -> TorchState.On
            else -> TorchState.Off
        })
        delay(500)
    }
}.distinctUntilChanged()
