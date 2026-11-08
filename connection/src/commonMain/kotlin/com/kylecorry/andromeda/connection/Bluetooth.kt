package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import com.juul.kable.Peripheral
import kotlinx.coroutines.flow.Flow

/** Scans for Bluetooth Low Energy advertisements while the returned flow is collected.
 * Platform Bluetooth permissions must be granted before collecting.
 */
fun scanBluetooth(): Flow<Advertisement> = Scanner().advertisements


/** Connects to an advertisement and returns a Kable peripheral owned by the caller.
 * Close the returned peripheral when it is no longer needed. Failed and cancelled
 * connection attempts dispose the peripheral automatically.
 */
suspend fun Advertisement.connectBluetooth(): Peripheral {
    val peripheral = Peripheral(this)
    try {
        peripheral.connect()
        return peripheral
    } catch (error: Throwable) {
        peripheral.close()
        throw error
    }
}

/** Connects for the duration of [block], closing the peripheral even if it fails or is cancelled. */
suspend fun <T> Advertisement.withBluetoothConnection(block: suspend Peripheral.() -> T): T {
    val peripheral = connectBluetooth()
    try {
        return peripheral.block()
    } finally {
        peripheral.close()
    }
}
