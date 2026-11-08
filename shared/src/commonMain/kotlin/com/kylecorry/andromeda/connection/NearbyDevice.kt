package com.kylecorry.andromeda.connection

import com.kylecorry.andromeda.core.sensors.Sensor

interface NearbyDevice : Sensor {
    val name: String
    val address: String
    val isConnected: Boolean
    val isConnecting: Boolean
    val messages: List<NearbyDeviceMessage>
    var messageHistorySize: Int

    suspend fun send(message: ByteArray)
}