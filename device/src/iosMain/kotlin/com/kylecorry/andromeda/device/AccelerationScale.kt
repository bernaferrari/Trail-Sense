package com.kylecorry.andromeda.device

// Core Motion reports acceleration in g with the opposite sign to Android's sensor convention.
internal actual fun accelerationScale(): Float = -9.80665f
