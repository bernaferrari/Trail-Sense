package com.kylecorry.andromeda.connection

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.ksensor.core.context.KSensorContext
import java.io.Closeable
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SuppressLint("MissingPermission")
actual suspend fun connectWifiDirect(peer: WifiDirectPeer): WifiDirectGroup = formWifiDirectGroup(peer)

actual suspend fun awaitWifiDirectGroup(): WifiDirectGroup = formWifiDirectGroup(null)

@SuppressLint("MissingPermission")
private suspend fun formWifiDirectGroup(peer: WifiDirectPeer?): WifiDirectGroup {
    val context = KSensorContext.get().applicationContext
    val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        ?: throw UnsupportedOperationException("Wi-Fi Direct is unavailable")
    val channel = manager.initialize(context, Looper.getMainLooper(), null)
        ?: error("Unable to initialize Wi-Fi Direct")
    try {
        val info = callbackFlow<WifiP2pInfo> {
            fun requestInfo() = manager.requestConnectionInfo(channel) {
                if (it.groupFormed && it.groupOwnerAddress != null) trySend(it)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = requestInfo()
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION), ContextCompat.RECEIVER_EXPORTED)
            try {
                if (peer != null) {
                    val config = WifiP2pConfig().apply { deviceAddress = peer.address }
                    manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() = requestInfo()
                        override fun onFailure(reason: Int) { close(IllegalStateException("Wi-Fi Direct connection failed: $reason")) }
                    })
                } else requestInfo()
                awaitClose()
            } finally {
                context.unregisterReceiver(receiver)
            }
        }.first()
        return AndroidWifiDirectGroup(manager, channel, info.groupOwnerAddress.hostAddress!!, info.isGroupOwner)
    } catch (error: Throwable) {
        if (peer != null) runCatching { manager.cancelConnect(channel, null) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) channel.close()
        throw error
    }
}

private class AndroidWifiDirectGroup(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    override val ownerAddress: String,
    override val isOwner: Boolean,
) : WifiDirectGroup {
    private val lock = Any()
    private var closed = false
    private val resources = mutableSetOf<Closeable>()

    private fun track(resource: Closeable) = synchronized(lock) {
        if (closed) {
            resource.close()
            error("Wi-Fi Direct group is closed")
        }
        resources.add(resource)
    }

    private fun forget(resource: Closeable) = synchronized(lock) { resources.remove(resource) }

    override suspend fun openConnection(port: Int): WifiDirectConnection {
        require(port in 1..65535)
        return suspendCancellableCoroutine { continuation ->
            val pending = AtomicReference<Closeable?>()
            val task = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val socket = if (isOwner) {
                        val server = ServerSocket(port)
                        pending.set(server)
                        track(server)
                        try { server.accept() } finally { forget(server); server.close() }
                    } else {
                        var connected: Socket? = null
                        var failure: IOException? = null
                        repeat(60) {
                            if (connected == null && continuation.isActive) {
                                val attempt = Socket()
                                pending.set(attempt)
                                track(attempt)
                                try {
                                    attempt.connect(InetSocketAddress(ownerAddress, port), 1000)
                                    connected = attempt
                                } catch (error: IOException) {
                                    failure = error
                                    forget(attempt)
                                    attempt.close()
                                    Thread.sleep(500)
                                }
                            }
                        }
                        connected ?: throw failure ?: IOException("Connection cancelled")
                    }
                    pending.set(socket)
                    track(socket)
                    val connection = AndroidWifiDirectConnection(socket) { forget(socket) }
                    continuation.resume(connection) { _, value, _ -> value.close() }
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
            continuation.invokeOnCancellation {
                runCatching { pending.getAndSet(null)?.close() }
                task.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun close() {
        val toClose = synchronized(lock) {
            if (closed) return
            closed = true
            resources.toList().also { resources.clear() }
        }
        toClose.forEach { runCatching { it.close() } }
        runCatching { manager.removeGroup(channel, null) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) channel.close()
    }
}

private class AndroidWifiDirectConnection(
    private val socket: Socket,
    private val onClose: () -> Unit,
) : WifiDirectConnection {
    private val closed = AtomicBoolean(false)
    private val readLock = Mutex()
    private val writeLock = Mutex()

    override suspend fun read(maxBytes: Int): ByteArray? = readLock.withLock {
        require(maxBytes > 0)
        operation {
            val bytes = ByteArray(maxBytes)
            val count = socket.inputStream.read(bytes)
            if (count < 0) null else bytes.copyOf(count)
        }
    }

    override suspend fun write(bytes: ByteArray) = writeLock.withLock {
        operation { socket.outputStream.write(bytes) }
    }

    private suspend fun <T> operation(block: () -> T): T = suspendCancellableCoroutine { continuation ->
        val task = CoroutineScope(Dispatchers.IO).launch {
            try {
                check(!closed.get()) { "Connection is closed" }
                continuation.resume(block())
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
        continuation.invokeOnCancellation { close(); task.cancel() }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try { socket.close() } finally { onClose() }
        }
    }
}
