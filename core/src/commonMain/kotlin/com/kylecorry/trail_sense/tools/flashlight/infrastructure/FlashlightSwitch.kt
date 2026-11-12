package com.kylecorry.trail_sense.tools.flashlight.infrastructure

/** The light itself, which the flashlight modes switch on and off. */
interface FlashlightSwitch {
    fun turnOn()
    fun turnOff()
}
