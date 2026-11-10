package com.kylecorry.andromeda.ipc.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.ipc.CODE_INTERNAL_ERROR
import com.kylecorry.andromeda.ipc.CODE_SERVICE_UNAVAILABLE
import com.kylecorry.andromeda.ipc.IpcRequest
import com.kylecorry.andromeda.ipc.IpcClient
import com.kylecorry.andromeda.ipc.IpcResponse
import com.kylecorry.andromeda.ipc.toBundle
import com.kylecorry.andromeda.ipc.toIpcHeaders
import com.kylecorry.andromeda.ipc.PROPERTY_CODE
import com.kylecorry.andromeda.ipc.PROPERTY_HEADERS
import com.kylecorry.andromeda.ipc.PROPERTY_PAYLOAD
import com.kylecorry.andromeda.ipc.PROPERTY_ROUTE
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlin.coroutines.resume

public class MessengerIpcClient(
    private val context: Context,
    private val intent: Intent,
    private val looper: Looper = Looper.getMainLooper()
) : AutoCloseable, IpcClient {
    private var isBound = false
    private var isConnecting = false
    private var isBindingRegistered = false
    private var lock = Any()

    private var messenger: Messenger? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            synchronized(lock) {
                messenger = Messenger(binder)
                isBound = true
                isConnecting = false
                isBindingRegistered = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(lock) {
                messenger = null
                isBound = false
                isConnecting = false
            }
        }
    }

    public fun connect(): Boolean {
        synchronized(lock) {
            if (isConnecting || isBound) {
                return true
            }
            val success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (success) {
                isBindingRegistered = true
                if (!isBound) {
                    isConnecting = true
                }
            } else {
                isConnecting = false
            }
            return success
        }
    }

    public fun isConnected(): Boolean {
        synchronized(lock) {
            return isBound
        }
    }

    public suspend fun waitUntilConnected(timeoutMillis: Long = 1000): Unit = withContext(Dispatchers.Default) {
        var isNowConnected = false
        synchronized(lock) {
            if (!isConnecting && !isBound) {
                throw IllegalStateException("Service is not connected")
            }
            isNowConnected = isBound
        }

        withTimeout(timeoutMillis) {
            while (!isNowConnected) {
                synchronized(lock) {
                    isNowConnected = isBound
                }
                delay(20)
            }
        }
    }

    override suspend fun send(
        path: String,
        request: IpcRequest,
    ): IpcResponse =
        suspendCancellableCoroutine {
            val message = Message.obtain()
            val bundle = message.data
            bundle.putString(PROPERTY_ROUTE, path)
            bundle.putBundle(PROPERTY_HEADERS, request.headers.toBundle())
            bundle.putByteArray(PROPERTY_PAYLOAD, request.payload)

            val replyHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    val result = runCatching {
                        IpcResponse(
                            msg.data.getInt(PROPERTY_CODE, CODE_INTERNAL_ERROR),
                            (msg.data.getBundle(PROPERTY_HEADERS) ?: bundleOf()).toIpcHeaders(),
                            msg.data.getByteArray(PROPERTY_PAYLOAD)
                        )
                    }
                    if (it.isActive) it.resumeWith(result)
                }
            }

            val replyMessenger = Messenger(replyHandler)
            message.replyTo = replyMessenger
            messenger?.send(message) ?: it.resume(
                IpcResponse(
                    CODE_SERVICE_UNAVAILABLE,
                    emptyMap(),
                    null
                )
            )
        }

    public suspend fun connectAndSend(
        route: String,
        request: IpcRequest = IpcRequest(),
        connectTimeout: Duration = 10.seconds,
        requestTimeout: Duration = 1.minutes,
        stayConnected: Boolean = false
    ): IpcResponse {
        return try {
            val success = connect()
            if (!success) {
                throw IllegalStateException("Could not connect to service")
            }
            waitUntilConnected(connectTimeout.inWholeMilliseconds)
            withTimeout(requestTimeout.inWholeMilliseconds) {
                send(route, request)
            }
        } finally {
            if (!stayConnected) {
                close()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (!isBindingRegistered) {
                return
            }
            context.unbindService(connection)
            messenger = null
            isBound = false
            isConnecting = false
            isBindingRegistered = false
        }
    }
}
