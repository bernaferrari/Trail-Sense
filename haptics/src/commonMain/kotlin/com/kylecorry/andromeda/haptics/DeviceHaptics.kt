package com.kylecorry.andromeda.haptics

/** Returns the platform haptic motor. Check [HapticMotor.isAvailable] before use. */
expect fun deviceHaptics(): HapticMotor
