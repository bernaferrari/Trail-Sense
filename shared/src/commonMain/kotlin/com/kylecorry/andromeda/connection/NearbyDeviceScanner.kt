package com.kylecorry.andromeda.connection

import com.kylecorry.andromeda.core.sensors.Sensor

interface NearbyDeviceScanner : Sensor {
    val devices: List<NearbyDevice>
}