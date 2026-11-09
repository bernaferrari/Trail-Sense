package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.trail_sense.shared.time.today

import com.kylecorry.trail_sense.shared.time.toLocalDate

import com.kylecorry.luna.specifications.Specification
import kotlin.time.Instant

class IsTodaySpecification : Specification<Instant>() {
    override fun isSatisfiedBy(value: Instant): Boolean {
        return value.toLocalDate() == today()
    }
}
