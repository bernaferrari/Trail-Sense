package com.kylecorry.andromeda.connection

import kotlinx.coroutines.flow.Flow

data class WifiDirectPeer(val address: String, val name: String)

/** Whether this platform supports Wi-Fi Direct peer discovery. */
expect val wifiDirectAvailable: Boolean

/**
 * Discovers nearby peers while collected. On Android the host must initialize KSensor's
 * application context, declare Wi-Fi state permissions, and grant location and, on
 * Android 13+, nearby Wi-Fi permissions.
 * This exposes discovery only; it does not establish a connection.
 */
expect fun scanWifiDirect(): Flow<List<WifiDirectPeer>>
