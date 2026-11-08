package com.kylecorry.andromeda.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraSurface(modifier: Modifier, onReady: (PhotoCamera?) -> Unit, onError: (Throwable) -> Unit) =
    CameraSurfaceImpl(modifier, onReady, onError)
