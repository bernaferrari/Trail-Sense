package com.kylecorry.andromeda.connection

import kotlinx.coroutines.flow.Flow

public data class WifiDirectPeer(val address: String, val name: String)

/** Whether this platform supports Wi-Fi Direct peer discovery. */
public expect val wifiDirectAvailable: Boolean

/**
 * Discovers nearby peers while collected. On Android the host must initialize KSensor's
 * application context and declare and grant location and, on Android 13+, nearby Wi-Fi permissions.
 * This exposes discovery only; it does not establish a connection.
 */
public expect fun scanWifiDirect(): Flow<List<WifiDirectPeer>>

/** A Wi-Fi Direct group. Both peers must call [openConnection] with the same port. */
public interface WifiDirectGroup : AutoCloseable {
    public val ownerAddress: String
    public val isOwner: Boolean

    /** Opens a bidirectional byte stream; cancellation closes the pending socket. */
    public suspend fun openConnection(port: Int = 8888): WifiDirectConnection
}

/** A connected byte stream. Close it when finished. */
public interface WifiDirectConnection : ByteConnection

/** Forms a group with a discovered peer. The operating system may ask the user to confirm. */
public expect suspend fun connectWifiDirect(peer: WifiDirectPeer): WifiDirectGroup

/** Waits for an incoming group invitation and for the group to be formed. */
public expect suspend fun awaitWifiDirectGroup(): WifiDirectGroup
