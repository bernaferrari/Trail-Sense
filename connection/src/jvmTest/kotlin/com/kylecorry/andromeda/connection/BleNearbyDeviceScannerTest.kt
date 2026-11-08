package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.characteristicOf
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class BleNearbyDeviceScannerTest {
    @Test
    fun discoversUniqueMatchingDevicesAndStopsScanning() = runBlocking {
        fun advertisement(name: String): Advertisement = Proxy.newProxyInstance(
            Advertisement::class.java.classLoader, arrayOf(Advertisement::class.java)
        ) { _, method, _ -> if (method.name == "getName") name else null } as Advertisement
        val packets = MutableSharedFlow<Advertisement>()
        val characteristic = characteristicOf(
            Uuid.parse("0000180f-0000-1000-8000-00805f9b34fb"),
            Uuid.parse("00002a19-0000-1000-8000-00805f9b34fb")
        )
        val found = CompletableDeferred<Unit>()
        val scanner = BleNearbyDeviceScanner(
            this, characteristic, characteristic,
            matches = { it.name != "ignored" },
            advertisements = { packets },
            identity = { it.name.orEmpty() },
        )
        val listener = { if (scanner.devices.size == 2) found.complete(Unit); true }
        scanner.start(listener)
        withTimeout(1.seconds) { packets.subscriptionCount.first { it > 0 } }
        packets.emit(advertisement("first"))
        packets.emit(advertisement("first"))
        packets.emit(advertisement("ignored"))
        packets.emit(advertisement("second"))
        withTimeout(1.seconds) { found.await() }
        assertTrue(scanner.hasValidReading)
        assertEquals(listOf("first", "second"), scanner.devices.map { it.name })
        scanner.stop(listener)
        assertFalse(scanner.isScanning)
        assertFalse(scanner.hasValidReading)
    }
}
