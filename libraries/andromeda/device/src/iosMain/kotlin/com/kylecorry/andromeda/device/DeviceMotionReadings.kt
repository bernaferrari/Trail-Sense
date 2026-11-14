package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.units.Reading
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.Foundation.NSOperationQueue
import kotlin.time.Clock
import kotlin.time.Duration

internal actual fun platformGravity(interval: Duration): Flow<Reading<Vector3>> = deviceMotion(interval, true)
internal actual fun platformLinearAcceleration(interval: Duration): Flow<Reading<Vector3>> = deviceMotion(interval, false)

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformAttitude(interval: Duration): Flow<Reading<DeviceAttitude>> = callbackFlow {
    val manager = CMMotionManager()
    val reference = CMAttitudeReferenceFrameXMagneticNorthZVertical
    if (!manager.deviceMotionAvailable || CMMotionManager.availableAttitudeReferenceFrames() and reference == 0uL) {
        close()
        return@callbackFlow
    }
    manager.deviceMotionUpdateInterval = interval.inWholeMilliseconds.coerceAtLeast(1) / 1000.0
    manager.startDeviceMotionUpdatesUsingReferenceFrame(reference, NSOperationQueue.mainQueue) { motion, error ->
        if (error != null) {
            close(IllegalStateException(error.localizedDescription))
        } else if (motion != null) {
            val quaternion = motion.attitude.quaternion.useContents {
                // Core Motion's magnetic frame is north–west–up; rotate it into east–north–up.
                northWestUpToEastNorthUp(Quaternion(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
            }
            trySend(Reading(DeviceAttitude(quaternion), Clock.System.now()))
        }
    }
    awaitClose { manager.stopDeviceMotionUpdates() }
}.flowOn(Dispatchers.Main)

@OptIn(ExperimentalForeignApi::class)
private fun deviceMotion(interval: Duration, gravity: Boolean): Flow<Reading<Vector3>> = callbackFlow {
    val manager = CMMotionManager()
    if (!manager.deviceMotionAvailable) { close(); return@callbackFlow }
    manager.deviceMotionUpdateInterval = interval.inWholeMilliseconds.coerceAtLeast(1) / 1000.0
    manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, error ->
        if (error != null) {
            close(IllegalStateException(error.localizedDescription))
        } else if (motion != null) {
            val acceleration = if (gravity) motion.gravity else motion.userAcceleration
            val vector = acceleration.useContents { Vector3(x.toFloat(), y.toFloat(), z.toFloat()) }
            trySend(Reading(Vector3(vector.x * -9.80665f, vector.y * -9.80665f, vector.z * -9.80665f), Clock.System.now()))
        }
    }
    awaitClose { manager.stopDeviceMotionUpdates() }
}.flowOn(Dispatchers.Main)
