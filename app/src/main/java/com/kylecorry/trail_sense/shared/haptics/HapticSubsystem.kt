package com.kylecorry.trail_sense.shared.haptics

import kotlin.time.Duration.Companion.seconds

import com.kylecorry.trail_sense.shared.time.JavaTime.toKotlin

import com.kylecorry.trail_sense.shared.time.interop.*

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.haptics.DialHapticFeedback
import com.kylecorry.andromeda.haptics.HapticFeedbackType
import com.kylecorry.andromeda.haptics.HapticMotor
import com.kylecorry.andromeda.haptics.deviceHaptics
import java.time.Duration

class HapticSubsystem private constructor(context: Context) {

    private val motor = deviceHaptics()

    fun off() {
        motor.off()
    }

    fun tick() {
        motor.feedback(HapticFeedbackType.Tick)
    }

    fun interval(on: Duration, off: Duration = on) {
        motor.interval(on.toKotlin(), off.toKotlin())
    }

    fun click() {
        motor.feedback(HapticFeedbackType.Click)
    }

    fun alert() {
        motor.once(1.seconds)
    }

    fun dial(frequency: Int = 1): DialHapticFeedback {
        return DialHapticFeedback(motor, frequency)
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: HapticSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): HapticSubsystem {
            if (instance == null) {
                instance = HapticSubsystem(context.applicationContext)
            }
            return instance!!
        }

    }

}
