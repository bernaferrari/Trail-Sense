package com.kylecorry.andromeda.connection

import com.kylecorry.andromeda.core.sensors.Sensor

public interface NearbyDeviceScanner : Sensor {
    public val devices: List<NearbyDevice>
}