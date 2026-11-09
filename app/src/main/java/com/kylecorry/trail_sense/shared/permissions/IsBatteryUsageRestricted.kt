package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.shared.andromeda_temp.permissions.Permissions
class IsBatteryUsageRestricted : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        return !Permissions.isIgnoringBatteryOptimizations(value)
    }
}
