package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.andromeda.sense.location.SatelliteStatusSensor
interface ISatelliteGPS : GpsSensor, SatelliteStatusSensor
