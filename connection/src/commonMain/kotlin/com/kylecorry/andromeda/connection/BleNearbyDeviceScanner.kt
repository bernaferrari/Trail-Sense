package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.WriteType
import com.kylecorry.andromeda.core.sensors.SharedSensor
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Discovers BLE peripherals and exposes each as a nearby device. */
public class BleNearbyDeviceScanner(
    private val scope: CoroutineScope,
    private val receiveCharacteristic: Characteristic,
    private val sendCharacteristic: Characteristic,
    private val writeType: WriteType = WriteType.WithResponse,
    private val matches: (Advertisement) -> Boolean = { true },
    private val advertisements: () -> Flow<Advertisement> = ::scanBluetooth,
    private val identity: (Advertisement) -> String = { "${it.identifier}" },
) : SharedSensor(), NearbyDeviceScanner {
    private val lock = SynchronizedObject()
    private val found = linkedMapOf<String, BleNearbyDevice>()
    private val mutableFailure = MutableStateFlow<Throwable?>(null)
    private var valid = false
    private var scanning = false
    private var session = 0L
    private var job: Job? = null

    /** The most recent scan failure, cleared when scanning restarts. */
    public val failure: StateFlow<Throwable?> = mutableFailure.asStateFlow()
    public val isScanning: Boolean get() = synchronized(lock) { scanning }
    /** True after the first matching advertisement is received in this scan. */
    override val hasValidReading: Boolean get() = synchronized(lock) { valid }
    override val devices: List<NearbyDevice> get() = synchronized(lock) { found.values.toList() }

    override fun startImpl() {
        val current = synchronized(lock) {
            session++
            found.clear()
            valid = false
            scanning = true
            session
        }
        mutableFailure.value = null
        resetSessionMetadata()
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                advertisements().collect { advertisement ->
                    if (!matches(advertisement)) return@collect
                    val id = identity(advertisement)
                    val added = synchronized(lock) {
                        if (session != current || id in found) false else {
                            found[id] = BleNearbyDevice(advertisement, receiveCharacteristic, sendCharacteristic, scope, writeType)
                            valid = true
                            true
                        }
                    }
                    if (added) {
                        setEventTime(Clock.System.now())
                        notifyListeners()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (synchronized(lock) { session == current }) mutableFailure.value = error
            }
        }
        job?.invokeOnCompletion {
            val changed = synchronized(lock) {
                if (session != current) false else {
                    val wasScanning = scanning
                    scanning = false
                    wasScanning
                }
            }
            if (changed) notifyListeners()
        }
        job?.start()
    }

    override fun stopImpl() {
        synchronized(lock) {
            session++
            scanning = false
            valid = false
        }
        job?.cancel()
        job = null
    }
}
