package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
public actual fun CameraSurface(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit, frameAnalysis: Boolean): Unit =
    CameraSurfaceImpl(modifier, onReady, onError, frameAnalysis)
