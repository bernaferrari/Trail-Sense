package com.kylecorry.andromeda.haptics

/** Returns the platform haptic motor. Check [IHapticMotor.isAvailable] before use. */
expect fun deviceHaptics(): IHapticMotor
