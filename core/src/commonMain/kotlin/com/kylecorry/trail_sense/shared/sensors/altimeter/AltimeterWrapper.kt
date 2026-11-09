package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.Altimeter
interface AltimeterWrapper : Altimeter {
    val altimeter: Altimeter
    val altitudeAccuracy: Float?
}
