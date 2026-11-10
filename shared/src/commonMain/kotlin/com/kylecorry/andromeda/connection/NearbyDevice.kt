package com.kylecorry.andromeda.connection

import com.kylecorry.andromeda.core.sensors.Sensor

public interface NearbyDevice : Sensor {
    public val name: String
    public val address: String
    public val isConnected: Boolean
    public val isConnecting: Boolean
    public val messages: List<NearbyDeviceMessage>
    public var messageHistorySize: Int

    public suspend fun send(message: ByteArray)
}