package com.kylecorry.andromeda.core.system

import android.os.Build

actual fun deviceInfo(): DeviceInfo = DeviceInfo("Android", Build.VERSION.RELEASE, Build.MODEL, Build.MANUFACTURER, Build.VERSION.SDK_INT)
