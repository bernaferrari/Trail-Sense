package com.kylecorry.andromeda.connection

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.ksensor.core.context.KSensorContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

public actual val wifiDirectAvailable: Boolean
    get() = KSensorContext.get().getSystemService(Context.WIFI_P2P_SERVICE) is WifiP2pManager

@SuppressLint("MissingPermission")
public actual fun scanWifiDirect(): Flow<List<WifiDirectPeer>> = callbackFlow {
    val context = KSensorContext.get().applicationContext
    val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        ?: throw UnsupportedOperationException("Wi-Fi Direct is unavailable")
    val channel = manager.initialize(context, Looper.getMainLooper(), null)
        ?: error("Unable to initialize Wi-Fi Direct")
    fun requestPeers() {
        manager.requestPeers(channel) { peers ->
            trySend(peers.deviceList.map { WifiDirectPeer(it.deviceAddress, it.deviceName.orEmpty()) })
        }
    }
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { requestPeers() }
    }
    ContextCompat.registerReceiver(context, receiver, IntentFilter(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION), ContextCompat.RECEIVER_EXPORTED)
    try {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = requestPeers()
            override fun onFailure(reason: Int) { close(IllegalStateException("Wi-Fi Direct discovery failed: $reason")) }
        })
        awaitClose()
    } finally {
        context.unregisterReceiver(receiver)
        runCatching { manager.stopPeerDiscovery(channel, null) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) channel.close()
    }
}
