package com.kylecorry.andromeda.sound

import com.kylecorry.andromeda.core.sensors.Sensor

interface IMicrophone: Sensor {
    val audio: ShortArray?
    val amplitude: Short?
}