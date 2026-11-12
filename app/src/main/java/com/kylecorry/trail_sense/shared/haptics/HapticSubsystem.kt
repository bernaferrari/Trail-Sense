package com.kylecorry.trail_sense.shared.haptics

import kotlin.time.Duration.Companion.seconds

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.haptics.DialHapticFeedback
import com.kylecorry.andromeda.haptics.HapticFeedbackType
import com.kylecorry.andromeda.haptics.deviceHaptics
import kotlin.time.Duration

class HapticSubsystem private constructor(context: Context) : IHapticSubsystem {

    private val motor = deviceHaptics()

    override fun off() {
        motor.off()
    }

    override fun tick() {
        motor.feedback(HapticFeedbackType.Tick)
    }

    override fun interval(on: Duration, off: Duration) {
        motor.interval(on, off)
    }

    override fun click() {
        motor.feedback(HapticFeedbackType.Click)
    }

    override fun alert() {
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
