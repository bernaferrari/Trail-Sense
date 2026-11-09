package com.kylecorry.andromeda.ipc.server

import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.ipc.CODE_BAD_REQUEST
import com.kylecorry.andromeda.ipc.CODE_INTERNAL_ERROR
import com.kylecorry.andromeda.ipc.IpcRequest
import com.kylecorry.andromeda.ipc.IpcResponse
import com.kylecorry.andromeda.ipc.IpcRouter
import com.kylecorry.andromeda.ipc.PROPERTY_CODE
import com.kylecorry.andromeda.ipc.PROPERTY_HEADERS
import com.kylecorry.andromeda.ipc.PROPERTY_PAYLOAD
import com.kylecorry.andromeda.ipc.PROPERTY_ROUTE
import com.kylecorry.andromeda.ipc.toBundle
import com.kylecorry.andromeda.ipc.toIpcHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Android Messenger adapter for a shared [IpcRouter]. */
public class MessengerIpcRouter(
    private val router: IpcRouter,
    private val looper: Looper = Looper.getMainLooper(),
    private val queueSize: Int = 256,
) : AutoCloseable {
    private var messenger: Messenger? = null
    private var scope: CoroutineScope? = null

    init { require(queueSize > 0) }

    public fun bind(): IBinder? {
        unbind()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = Channel<Message>(queueSize)
        scope = workerScope
        workerScope.launch {
            for (message in queue) {
                val data = message.data
                val response = try {
                    val route = data.getString(PROPERTY_ROUTE)
                    if (route == null) IpcResponse(CODE_BAD_REQUEST)
                    else router.send(route, IpcRequest(
                        (data.getBundle(PROPERTY_HEADERS) ?: bundleOf()).toIpcHeaders(),
                        data.getByteArray(PROPERTY_PAYLOAD),
                    ))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e("MessengerIpcRouter", "Request failed", error)
                    IpcResponse(CODE_INTERNAL_ERROR)
                }
                val reply = Message.obtain().apply {
                    data.putInt(PROPERTY_CODE, response.code)
                    data.putBundle(PROPERTY_HEADERS, response.headers.toBundle())
                    data.putByteArray(PROPERTY_PAYLOAD, response.payload)
                }
                try {
                    message.replyTo?.send(reply)
                } catch (error: RemoteException) {
                    Log.w("MessengerIpcRouter", "Unable to send reply", error)
                }
            }
        }
        messenger = Messenger(object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                // Copy the Message because Handler recycles it when this callback returns.
                val copy = Message.obtain(msg)
                workerScope.launch { queue.send(copy) }
            }
        })
        return messenger?.binder
    }

    public fun unbind() {
        messenger = null
        scope?.cancel()
        scope = null
    }

    override fun close(): Unit = unbind()
}
