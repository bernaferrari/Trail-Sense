package com.kylecorry.trail_sense.test_utils.views

import androidx.test.uiautomator.UiObject2
import android.graphics.Point
import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.test_utils.TestUtils.getMatchingChild

fun TestView.click(
    durationMillis: Long? = null,
    xPercent: Float? = null,
    yPercent: Float? = null
): TestView {

    val point = if (xPercent != null && yPercent != null) {
        val bounds = uiObject.visibleBounds
        Point(
            bounds.left + (bounds.width() * xPercent).toInt(),
            bounds.top + (bounds.height() * yPercent).toInt()
        )
    } else {
        null
    }

    if (point == null) {
        if (durationMillis != null) {
            uiObject.click(durationMillis)
        } else {
            uiObject.click()
        }
    } else {
        if (durationMillis != null) {
            uiObject.click(point, durationMillis)
        } else {
            uiObject.click(point)
        }
    }

    return this
}

fun TestView.longClick(): TestView {
    uiObject.longClick()
    return this
}

fun TestView.input(text: String, checkDescendants: Boolean = false): TestView {
    if (!checkDescendants) {
        setTextAndCheck(uiObject, text)
    } else {
        // A Compose text field draws its label inside the field, so the field may be a parent of the matched text
        val field = if (uiObject.className == EDIT_TEXT) {
            uiObject
        } else {
            getMatchingChild(uiObject) { it.className == EDIT_TEXT } ?: getMatchingAncestor(uiObject) {
                it.className == EDIT_TEXT
            }
        } ?: throw AssertionError("No text field found for $uiObject")
        setTextAndCheck(field, text)
    }
    return this
}

/**
 * UiAutomator's setText sends the accessibility set-text action and returns without knowing whether the field took
 * it. A Compose text field that has only just appeared (a dialog opening on a busy emulator) can drop it, which left
 * the field empty and the test failing several steps later. Send it again until the field has some text, and fail
 * here (so the caller's waitFor retries) if it never does. Fields may reformat what they are given, so only "still
 * empty" counts as not taken.
 */
private fun setTextAndCheck(field: UiObject2, text: String) {
    field.text = text
    if (text.isEmpty()) {
        return
    }
    repeat(SET_TEXT_ATTEMPTS) {
        Thread.sleep(SET_TEXT_SETTLE_MS)
        if (!field.text.isNullOrEmpty()) {
            return
        }
        field.text = text
    }
    throw AssertionError("The text field did not take \"$text\"")
}

private const val SET_TEXT_ATTEMPTS = 5
private const val SET_TEXT_SETTLE_MS = 200L

private const val EDIT_TEXT = "android.widget.EditText"

private fun getMatchingAncestor(obj: UiObject2, predicate: (UiObject2) -> Boolean): UiObject2? {
    var current = obj.parent
    while (current != null) {
        if (predicate(current)) {
            return current
        }
        current = current.parent
    }
    return null
}

fun TestView.scroll(direction: Direction = Direction.DOWN, percent: Float = 0.5f): Boolean {
    val scrolled = uiObject.scroll(direction, percent)
    // Wait after scrolling (Android 16 issue)
    Thread.sleep(500)
    return scrolled
}

fun TestView.scrollToEnd(direction: Direction = Direction.DOWN): TestView {
    while (uiObject.scroll(direction, 1f)) {
        // Do nothing
    }
    // Wait after scrolling (Android 16 issue)
    Thread.sleep(500)
    return this
}
