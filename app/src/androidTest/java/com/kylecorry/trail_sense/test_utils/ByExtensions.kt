package com.kylecorry.trail_sense.test_utils

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import java.util.regex.Pattern

fun byResId(id: Int, packageName: String? = null): BySelector {
    return byResName(
        TestUtils.context.resources.getResourceEntryName(id),
        packageName ?: TestUtils.context.resources.getResourcePackageName(id)
    )
}

/**
 * Matches a node by resource name. Compose test tags (exposed through testTagsAsResourceId) are
 * reported as the bare tag, while views report `<package>:id/<name>`, so both forms match.
 */
fun byResName(name: String, packageName: String? = null): BySelector {
    val pkg = packageName ?: AutomationLibrary.packageName ?: TestUtils.context.packageName
    return By.res(Pattern.compile("(" + Pattern.quote("$pkg:id/") + ")?" + Pattern.quote(name)))
}
