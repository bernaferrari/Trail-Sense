package com.kylecorry.andromeda.connection

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual val rfcommAvailable: Boolean = false
public actual suspend fun connectRfcomm(address: String, service: Uuid, security: RfcommSecurity): RfcommConnection =
    throw UnsupportedOperationException("Bluetooth Classic RFCOMM is unavailable on this platform")

public actual fun pairedBluetoothDevices(): List<PairedBluetoothDevice> = emptyList()
public actual suspend fun acceptRfcomm(name: String, service: Uuid, security: RfcommSecurity): RfcommPeer =
    throw UnsupportedOperationException("Bluetooth Classic RFCOMM is unavailable on this platform")

public actual fun scanClassicBluetooth(): Flow<DiscoveredBluetoothDevice> = flow {
    throw UnsupportedOperationException("Bluetooth Classic discovery is unavailable on this platform")
}
