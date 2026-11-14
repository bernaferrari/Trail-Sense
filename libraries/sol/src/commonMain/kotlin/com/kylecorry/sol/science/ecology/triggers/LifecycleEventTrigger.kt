package com.kylecorry.sol.science.ecology.triggers

import com.kylecorry.sol.science.ecology.LifecycleEventFactors

public interface LifecycleEventTrigger {
    public fun isTriggered(factors: LifecycleEventFactors): Boolean
    public fun reset() {
        // Do nothing
    }
}
