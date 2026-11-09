package com.kylecorry.andromeda.haptics

import com.ksensor.core.context.KSensorContext

actual fun deviceHaptics(): HapticMotor = AndroidHapticMotor(KSensorContext.get())
