package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.characteristicOf
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class BleNearbyDeviceTest {
    @Test
    fun receivesSendsAndCloses() = runBlocking {
        val states = MutableStateFlow<State>(State.Disconnected())
        val packets = MutableSharedFlow<ByteArray>()
        val closed = CompletableDeferred<Unit>()
        var sent = byteArrayOf()
        val peripheral = Proxy.newProxyInstance(Peripheral::class.java.classLoader, arrayOf(Peripheral::class.java)) { _, method, args ->
            when (method.name) {
                "getState" -> states
                "observe" -> packets
                "connect" -> { states.value = State.Connected(this); this }
                "write" -> { sent = (args[1] as ByteArray).copyOf(); Unit }
                "close" -> { closed.complete(Unit); Unit }
                else -> error("Unexpected peripheral call: ${method.name}")
            }
        } as Peripheral
        val advertisement = Proxy.newProxyInstance(Advertisement::class.java.classLoader, arrayOf(Advertisement::class.java)) { _, method, _ ->
            when (method.name) {
                "getName" -> "test"
                else -> null
            }
        } as Advertisement
        val characteristic = characteristicOf(
            Uuid.parse("0000180f-0000-1000-8000-00805f9b34fb"),
            Uuid.parse("00002a19-0000-1000-8000-00805f9b34fb")
        )
        val received = CompletableDeferred<Unit>()
        val device = BleNearbyDevice(advertisement, characteristic, characteristic, this, peripheralFactory = { peripheral })
        val listener = { if (device.messages.isNotEmpty()) received.complete(Unit); true }
        device.start(listener)
        withTimeout(1.seconds) { packets.subscriptionCount.first { it > 0 } }
        packets.emit(byteArrayOf(1, 2))
        withTimeout(1.seconds) { received.await() }
        assertEquals("test", device.name)
        assertContentEquals(byteArrayOf(1, 2), device.messages.single().message)
        device.send(byteArrayOf(3, 4))
        assertContentEquals(byteArrayOf(3, 4), sent)
        device.stop(listener)
        withTimeout(1.seconds) { closed.await() }
        assertFalse(device.isConnected)
    }
}
