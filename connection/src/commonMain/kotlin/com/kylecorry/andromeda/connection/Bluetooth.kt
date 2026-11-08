package com.kylecorry.andromeda.connection

import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.Flow

/** Scans for Bluetooth Low Energy advertisements while the returned flow is collected. */
fun scanBluetooth(): Flow<Advertisement> = Scanner().advertisements
