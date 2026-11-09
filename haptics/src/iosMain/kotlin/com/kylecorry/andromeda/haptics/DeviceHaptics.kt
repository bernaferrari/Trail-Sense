package com.kylecorry.andromeda.haptics

actual fun deviceHaptics(): HapticMotor = IosHapticMotor()
