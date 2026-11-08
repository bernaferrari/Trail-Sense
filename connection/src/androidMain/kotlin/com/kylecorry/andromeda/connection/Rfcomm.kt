package com.kylecorry.andromeda.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual val rfcommAvailable: Boolean
    @Suppress("DEPRECATION") get() = BluetoothAdapter.getDefaultAdapter() != null

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
actual suspend fun connectRfcomm(address: String, service: Uuid, security: RfcommSecurity): RfcommConnection {
    val adapter = checkNotNull(BluetoothAdapter.getDefaultAdapter()) { "Bluetooth is unavailable" }
    val device = adapter.getRemoteDevice(address)
    val uuid = UUID.fromString(service.toString())
    val socket = when (security) {
        RfcommSecurity.Secure -> device.createRfcommSocketToServiceRecord(uuid)
        RfcommSecurity.Insecure -> device.createInsecureRfcommSocketToServiceRecord(uuid)
    }
    val connection = AndroidRfcommConnection(socket)
    try {
        connection.runOperation { socket.connect() }
        return connection
    } catch (error: Throwable) {
        runCatching { connection.close() }
        throw error
    }
}

private class AndroidRfcommConnection(private val socket: BluetoothSocket) : RfcommConnection {
    private val closed = AtomicBoolean(false)
    private val readLock = Mutex()
    private val writeLock = Mutex()

    override suspend fun read(maxBytes: Int): ByteArray? = readLock.withLock {
        require(maxBytes > 0)
        runOperation {
            val buffer = ByteArray(maxBytes)
            val count = socket.inputStream.read(buffer)
            if (count < 0) null else buffer.copyOf(count)
        }
    }

    override suspend fun write(bytes: ByteArray) = writeLock.withLock {
        runOperation { socket.outputStream.write(bytes) }
    }

    suspend fun <T> runOperation(block: () -> T): T = suspendCancellableCoroutine { continuation ->
        val task = CoroutineScope(Dispatchers.IO).launch {
            try {
                check(!closed.get()) { "Bluetooth connection is closed" }
                val value = block()
                continuation.resume(value) { _, _, _ -> runCatching { close() } }
            } catch (error: Throwable) {
                continuation.resumeWithException(error)
            }
        }
        continuation.invokeOnCancellation {
            runCatching { close() }
            task.cancel()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) socket.close()
    }
}
