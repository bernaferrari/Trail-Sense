package com.kylecorry.andromeda.torch

import kotlinx.coroutines.flow.Flow

public enum class TorchState { Unavailable, Off, On }

/** Observes the camera torch selected by [deviceTorch], until collection is cancelled. */
public expect fun torchStates(): Flow<TorchState>
