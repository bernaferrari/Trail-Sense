package com.kylecorry.andromeda.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual val wifiDirectAvailable: Boolean get() = false
actual fun scanWifiDirect(): Flow<List<WifiDirectPeer>> = flow {
    throw UnsupportedOperationException("Wi-Fi Direct discovery is unavailable on this platform")
}

actual suspend fun connectWifiDirect(peer: WifiDirectPeer): WifiDirectGroup =
    throw UnsupportedOperationException("Wi-Fi Direct connections are unavailable on this platform")

actual suspend fun awaitWifiDirectGroup(): WifiDirectGroup =
    throw UnsupportedOperationException("Wi-Fi Direct connections are unavailable on this platform")
