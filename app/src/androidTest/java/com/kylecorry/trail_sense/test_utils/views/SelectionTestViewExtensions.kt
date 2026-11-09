package com.kylecorry.trail_sense.test_utils.views

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertEquals

/**
 * Checks the toggle state of the view. A tagged Compose row (such as a switch row) is not checkable itself,
 * so the state is read from its first checkable descendant.
 */
fun TestView.isChecked(isChecked: Boolean = true): TestView {
    assertEquals(isChecked, checkableNode(uiObject).isChecked)
    return this
}

private fun checkableNode(obj: UiObject2): UiObject2 {
    if (obj.isCheckable) {
        return obj
    }
    return obj.findObject(By.checkable(true)) ?: obj
}
