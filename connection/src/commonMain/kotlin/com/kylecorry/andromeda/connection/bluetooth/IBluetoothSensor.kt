package com.kylecorry.andromeda.connection.bluetooth

interface IBluetoothSensor {
    val messages: List<BluetoothMessage>
    val isConnected: Boolean

    /** Returns false when disconnected; a successful call does not confirm remote delivery. */
    fun write(data: String): Boolean
}