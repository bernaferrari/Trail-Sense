package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.byResId
import com.kylecorry.trail_sense.test_utils.byResName

fun quickAction(quickActionId: Int, @IdRes containerRes: Int? = null, index: Int = 0): TestView {
    var selector = byResName("quick_action_$quickActionId")
    if (containerRes != null) {
        selector =
            selector.hasAncestor(byResId(containerRes, packageName = AutomationLibrary.packageName))
    }
    return view(selector, index)
}
