package com.kylecorry.andromeda.connection

import kotlin.uuid.Uuid

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
