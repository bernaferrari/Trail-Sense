package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.DEFAULT_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.device
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import kotlin.math.abs

enum class Side {
    Left,
    Right
}

val TOOLBAR_LEFT_BUTTON_ID: Int = R.id.toolbar_left_button
val TOOLBAR_RIGHT_BUTTON_ID: Int = R.id.toolbar_right_button

/**
 * A button in a View-based toolbar, found by the toolbar's id.
 */
fun toolbarButton(@IdRes toolbarId: Int, side: Side): TestView {
    return waitFor(DEFAULT_WAIT_FOR_TIMEOUT) {
        view(
            toolbarId,
            if (side == Side.Left) TOOLBAR_LEFT_BUTTON_ID else TOOLBAR_RIGHT_BUTTON_ID,
            0,
            packageName = AutomationLibrary.packageName
        )
    }
}

/**
 * An action in the Compose top app bar of the current screen. The bar has no tags of its own, so the actions are
 * the clickable nodes on the back button's row, to the right of it. [Side.Right] is the last action and
 * [Side.Left] the first, matching the left and right buttons of the old View toolbar.
 */
fun toolbarButton(side: Side): TestView {
    return waitFor(DEFAULT_WAIT_FOR_TIMEOUT) {
        val actions = toolbarActions()
        if (actions.isEmpty()) {
            throw AssertionError("No toolbar actions found")
        }
        TestView(if (side == Side.Left) actions.first() else actions.last())
    }
}

private fun toolbarActions(): List<UiObject2> {
    val pkg = AutomationLibrary.packageName ?: TestUtils.context.packageName
    val clickable = device.findObjects(By.clickable(true).pkg(pkg))
    val back = device.findObject(By.desc(TestUtils.getString(R.string.back)).pkg(pkg))
    val minimumSize = (48 * TestUtils.context.resources.displayMetrics.density).toInt()
    val row = back?.visibleBounds ?: clickable
        .map { it.visibleBounds }
        .filter { it.top < device.displayHeight / 5 }
        .minByOrNull { it.top }
    ?: return emptyList()
    val tolerance = maxOf(row.height(), minimumSize) / 2
    return clickable
        .filter {
            val bounds = it.visibleBounds
            abs(bounds.centerY() - row.centerY()) <= tolerance &&
                    (back == null || bounds.left >= row.right)
        }
        .sortedBy { it.visibleBounds.left }
}
