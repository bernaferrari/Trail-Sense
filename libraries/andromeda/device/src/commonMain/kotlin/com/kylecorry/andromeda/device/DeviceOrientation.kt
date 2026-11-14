package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public enum class DeviceOrientation { Portrait, PortraitInverse, Landscape, LandscapeInverse, Flat, FlatInverse }

/** Classifies a gravity vector in the device coordinate system. */
public fun Vector3.deviceOrientation(): DeviceOrientation {
    val xMagnitude = abs(x)
    val yMagnitude = abs(y)
    val zMagnitude = abs(z)
    return when {
        xMagnitude >= yMagnitude && xMagnitude >= zMagnitude -> if (x < 0) DeviceOrientation.LandscapeInverse else DeviceOrientation.Landscape
        yMagnitude >= zMagnitude -> if (y < 0) DeviceOrientation.PortraitInverse else DeviceOrientation.Portrait
        z < 0 -> DeviceOrientation.FlatInverse
        else -> DeviceOrientation.Flat
    }
}

/** Emits the device's orientation from gravity readings where the platform provides them. */
public fun DeviceSensors.orientation(
    interval: Duration = 1.seconds,
    accuracy: SensorAccuracy = SensorAccuracy.Balanced,
): Flow<Reading<DeviceOrientation>> = gravity(interval, accuracy).map { Reading(it.value.deviceOrientation(), it.time) }
