package com.kylecorry.andromeda.torch

import com.ksensor.core.context.KSensorContext

actual fun deviceTorch(): ITorch = Torch(KSensorContext.get())
