package com.kylecorry.andromeda.torch

import kotlinx.coroutines.flow.Flow

enum class TorchState { Unavailable, Off, On }

/** Observes the camera torch selected by [deviceTorch], until collection is cancelled. */
expect fun torchStates(): Flow<TorchState>
