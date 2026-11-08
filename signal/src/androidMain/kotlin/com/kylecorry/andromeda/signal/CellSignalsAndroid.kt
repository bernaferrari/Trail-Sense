package com.kylecorry.andromeda.signal

import android.content.pm.PackageManager
import com.ksensor.core.context.KSensorContext
import com.kylecorry.andromeda.permissions.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

actual val cellularSignalSupported: Boolean
    get() = KSensorContext.get().packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

actual fun cellSignals(options: CellSignalOptions): Flow<List<CellSignal>> = callbackFlow {
    val context = KSensorContext.get()
    if (!Permissions.canGetFineLocation(context)) throw SecurityException("Fine location permission is required")
    val sensor = CellSignalSensor(context, options.updateCellCache, !options.includeUnregistered, options.pathLossFactor)
    var emitted = false
    val listener: () -> Boolean = {
        if (sensor.hasValidReading) {
            emitted = true
            trySend(sensor.signals.toList())
        }
        true
    }
    try {
        sensor.start(listener)
        if (!emitted && sensor.hasValidReading) trySend(sensor.signals.toList())
        awaitClose { }
    } finally {
        sensor.stop(listener)
    }
}.flowOn(Dispatchers.Main.immediate)
