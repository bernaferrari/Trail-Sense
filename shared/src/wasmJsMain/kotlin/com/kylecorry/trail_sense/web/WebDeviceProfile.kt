package com.kylecorry.trail_sense.web

import com.kylecorry.trail_sense.shared.device.DeviceProfile

internal class WebDeviceProfile : DeviceProfile {
    override val hasCompass: Boolean = false
    override val hasGyroscope: Boolean = false
    override val hasBarometer: Boolean = false
    override val hasHygrometer: Boolean = false
    override val hasDynamicColors: Boolean = false
    override val isMetricPreferred: Boolean = true
    override val uses24HourClock: Boolean = false
}
