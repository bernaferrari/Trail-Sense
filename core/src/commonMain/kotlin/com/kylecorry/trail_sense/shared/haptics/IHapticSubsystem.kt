package com.kylecorry.trail_sense.shared.haptics

import kotlin.time.Duration

/** The device's vibration motor. */
interface IHapticSubsystem {
    fun off()
    fun tick()
    fun click()
    fun interval(on: Duration, off: Duration = on)

    /** A single long buzz. */
    fun alert()
}
