package com.kylecorry.trail_sense.ios

import com.kylecorry.trail_sense.shared.device.DeviceProfile
import platform.CoreLocation.CLLocationManager
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleUsesMetricSystem
import platform.Foundation.NSNumber
import platform.Foundation.currentLocale

internal class IosDeviceProfile : DeviceProfile {
    private val motion = CMMotionManager()
    override val hasCompass: Boolean get() = CLLocationManager.headingAvailable()
    override val hasGyroscope: Boolean get() = motion.gyroAvailable
    override val hasBarometer: Boolean get() = CMAltimeter.isRelativeAltitudeAvailable()
    override val hasHygrometer: Boolean get() = false
    override val hasDynamicColors: Boolean get() = false
    override val isMetricPreferred: Boolean
        get() = (NSLocale.currentLocale.objectForKey(NSLocaleUsesMetricSystem) as? NSNumber)?.boolValue ?: true
    override val uses24HourClock: Boolean
        get() = NSDateFormatter().apply {
            locale = NSLocale.currentLocale
            setLocalizedDateFormatFromTemplate("j")
        }.dateFormat?.contains('a') == false
}
