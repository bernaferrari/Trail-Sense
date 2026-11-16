package com.kylecorry.andromeda.torch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

public actual fun deviceTorch(): Torch = UnavailableTorch

public actual fun screenBrightness(): ScreenBrightness? = null

public actual fun torchStates(): Flow<TorchState> = flowOf(TorchState.Unavailable)

private object UnavailableTorch : Torch {
    override val brightnessLevels: Int = 0
    override fun on() = Unit
    override fun on(brightness: Float) = Unit
    override fun off() = Unit
    override fun isAvailable(): Boolean = false
    override fun isDimmable(): Boolean = false
}
