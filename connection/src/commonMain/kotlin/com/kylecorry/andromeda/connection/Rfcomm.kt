package com.kylecorry.andromeda.connection

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/** A connected Bluetooth Classic byte stream. Close it when finished. */
interface RfcommConnection : AutoCloseable {
    /** Reads up to [maxBytes], or returns null when the peer closes the stream. */
    suspend fun read(maxBytes: Int = 4096): ByteArray?
    /** Writes all bytes to the connection. */
    suspend fun write(bytes: ByteArray)
}

enum class RfcommSecurity { Secure, Insecure }

/** Whether this platform implements outbound Bluetooth Classic RFCOMM. */
expect val rfcommAvailable: Boolean

/** Connects to a Bluetooth Classic service. Cancelling an operation closes its connection. */
expect suspend fun connectRfcomm(
    address: String,
    service: Uuid,
    security: RfcommSecurity = RfcommSecurity.Secure,
): RfcommConnection

/** Standard Bluetooth Serial Port Profile service identifier. */
val SerialPortService: Uuid = Uuid.parse("00001101-0000-1000-8000-00805f9b34fb")

data class PairedBluetoothDevice(val address: String, val name: String?)
data class DiscoveredBluetoothDevice(val address: String, val name: String?, val rssi: Int?, val bonded: Boolean)

/** Discovers Bluetooth Classic devices until the platform completes discovery or collection ends. */
expect fun scanClassicBluetooth(): Flow<DiscoveredBluetoothDevice>

data class RfcommPeer(val device: PairedBluetoothDevice, val connection: RfcommConnection)

/** Returns bonded Bluetooth Classic devices when supported by the platform. */
expect fun pairedBluetoothDevices(): List<PairedBluetoothDevice>

/** Waits for one incoming RFCOMM connection. Cancellation closes the listening socket. */
expect suspend fun acceptRfcomm(
    name: String,
    service: Uuid,
    security: RfcommSecurity = RfcommSecurity.Secure,
): RfcommPeer
