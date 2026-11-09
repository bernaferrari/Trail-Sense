package com.kylecorry.andromeda.core.system

import android.os.Build

public object Android {
    public val sdk: Int = Build.VERSION.SDK_INT
    public val version: String = Build.VERSION.RELEASE
    public val manufacturer: String = Build.MANUFACTURER
    public val device: String = Build.PRODUCT
    public val model: String = Build.MODEL
    public val fullDeviceName: String = "$manufacturer $device"
}