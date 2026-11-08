package com.kylecorry.andromeda.connection

import kotlin.uuid.Uuid

actual val rfcommAvailable: Boolean = false
actual suspend fun connectRfcomm(address: String, service: Uuid, security: RfcommSecurity): RfcommConnection =
    throw UnsupportedOperationException("Bluetooth Classic RFCOMM is unavailable on this platform")
