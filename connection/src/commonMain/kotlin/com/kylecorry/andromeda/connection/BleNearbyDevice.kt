package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.kylecorry.andromeda.core.sensors.SharedSensor
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * A nearby BLE device using characteristic notifications as messages.
 * Each notification is one message; [send] writes one characteristic value, subject to the
 * peripheral's negotiated write size. Subscribing connects; unsubscribing closes the connection.
 */
class BleNearbyDevice(
    private val advertisement: Advertisement,
    private val receiveCharacteristic: Characteristic,
    private val sendCharacteristic: Characteristic,
    private val scope: CoroutineScope,
    private val writeType: WriteType = WriteType.WithResponse,
    private val peripheralFactory: (Advertisement) -> Peripheral = { Peripheral(it) },
) : SharedSensor(), NearbyDevice {
    private val lock = SynchronizedObject()
    private val history = mutableListOf<NearbyDeviceMessage>()
    private var historyLimit = 1
    private var session = 0L
    private var peripheral: Peripheral? = null
    private var connected = false
    private var connecting = false
    private var job: Job? = null
    private val mutableFailure = MutableStateFlow<Throwable?>(null)

    /** Most recent connection or observation failure; cleared on the next connection attempt. */
    val failure: StateFlow<Throwable?> = mutableFailure.asStateFlow()
    override val name: String get() = advertisement.name ?: advertisement.peripheralName.orEmpty()
    override val address: String get() = advertisement.identifier.toString()
    override val isConnected: Boolean get() = synchronized(lock) { connected }
    override val isConnecting: Boolean get() = synchronized(lock) { connecting }
    override val hasValidReading: Boolean get() = isConnected
    override val messages: List<NearbyDeviceMessage> get() = synchronized(lock) {
        history.map { NearbyDeviceMessage(it.time, it.message.copyOf()) }
    }
    override var messageHistorySize: Int
        get() = synchronized(lock) { historyLimit }
        set(value) {
            require(value >= 0)
            synchronized(lock) {
                historyLimit = value
                while (history.size > historyLimit) history.removeAt(0)
            }
        }

    override fun startImpl() {
        val current = synchronized(lock) {
            session++
            connecting = true
            connected = false
            session
        }
        mutableFailure.value = null
        resetSessionMetadata()
        job = scope.launch(start = CoroutineStart.LAZY) {
            var device: Peripheral? = null
            try {
                val active = peripheralFactory(advertisement)
                device = active
                synchronized(lock) { if (session == current) peripheral = active }
                coroutineScope {
                    launch {
                        active.state.collect { state ->
                            val changed = synchronized(lock) {
                                if (session != current) false else {
                                    val newConnected = state is State.Connected
                                    val newConnecting = state is State.Connecting
                                    val result = connected != newConnected || connecting != newConnecting
                                    connected = newConnected
                                    connecting = newConnecting
                                    result
                                }
                            }
                            if (changed) notifyListeners()
                        }
                    }
                    launch {
                        active.observe(receiveCharacteristic).collect { bytes ->
                            val time = Clock.System.now()
                            val accepted = synchronized(lock) {
                                if (session != current) false else {
                                    if (historyLimit > 0) {
                                        history.add(NearbyDeviceMessage(time, bytes.copyOf()))
                                        while (history.size > historyLimit) history.removeAt(0)
                                    }
                                    true
                                }
                            }
                            if (accepted) {
                                setEventTime(time)
                                notifyListeners()
                            }
                        }
                    }
                    active.connect()
                    awaitCancellation()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (synchronized(lock) { session == current }) mutableFailure.value = error
            } finally {
                device?.close()
                val changed = synchronized(lock) {
                    if (session != current) false else {
                        peripheral = null
                        val wasActive = connected || connecting
                        connected = false
                        connecting = false
                        wasActive
                    }
                }
                if (changed) notifyListeners()
            }
        }
        job?.invokeOnCompletion {
            val changed = synchronized(lock) {
                if (session != current) false else {
                    val wasActive = connected || connecting
                    peripheral = null
                    connected = false
                    connecting = false
                    wasActive
                }
            }
            if (changed) notifyListeners()
        }
        job?.start()
    }

    override fun stopImpl() {
        synchronized(lock) {
            session++
            peripheral = null
            connected = false
            connecting = false
        }
        job?.cancel()
        job = null
    }

    override suspend fun send(message: ByteArray) {
        val active = synchronized(lock) {
            check(connected) { "BLE device is not connected" }
            checkNotNull(peripheral)
        }
        active.write(sendCharacteristic, message, writeType)
    }
}
