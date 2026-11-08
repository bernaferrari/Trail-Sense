package com.kylecorry.andromeda.haptics

import com.ksensor.core.context.KSensorContext

actual fun deviceHaptics(): IHapticMotor = HapticMotor(KSensorContext.get())
