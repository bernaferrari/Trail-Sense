package com.kylecorry.andromeda.torch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun torchStates(): Flow<TorchState> = flowOf(TorchState.Unavailable)
