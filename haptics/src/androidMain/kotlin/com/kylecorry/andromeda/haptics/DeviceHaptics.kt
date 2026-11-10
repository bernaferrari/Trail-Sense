package com.kylecorry.andromeda.haptics

import com.ksensor.core.context.KSensorContext

public actual fun deviceHaptics(): HapticMotor = AndroidHapticMotor(KSensorContext.get())
