package com.kylecorry.trail_sense.test_utils

import androidx.annotation.StringRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.TestView
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.getScrollableView
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.longClick
import com.kylecorry.trail_sense.test_utils.views.scroll
import com.kylecorry.trail_sense.test_utils.views.scrollToEnd
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithHint
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.util.regex.Pattern

object AutomationLibrary {

    var packageName: String? = null
    private var waitForTimeOverride: Long? = null

    private fun resolveWaitForTime(waitForTime: Long?): Long {
        if (waitForTime != null) {
            return waitForTime
        }
        return waitForTimeOverride ?: DEFAULT_WAIT_FOR_TIMEOUT
    }

    private inline fun withWaitForTimeOverride(waitForTime: Long, block: () -> Unit) {
        val previous = waitForTimeOverride
        waitForTimeOverride = waitForTime
        try {
            block()
        } finally {
            waitForTimeOverride = previous
        }
    }

    fun hasText(
        id: Int,
        text: String,
        ignoreCase: Boolean = false,
        checkDescendants: Boolean = true,
        exact: Boolean = false,
        index: Int = 0,
        childId: Int? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, childId = childId, index = index, packageName = packageName).hasText(
                text,
                ignoreCase = ignoreCase,
                checkDescendants = checkDescendants,
                contains = !exact
            )
        }
    }

    fun hasText(
        viewLookup: () -> TestView,
        text: String,
        ignoreCase: Boolean = false,
        checkDescendants: Boolean = true,
        exact: Boolean = false,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().hasText(
                text,
                ignoreCase = ignoreCase,
                checkDescendants = checkDescendants,
                contains = !exact
            )
        }
    }

    fun hasText(
        id: Int,
        checkDescendants: Boolean = true,
        message: String? = null,
        index: Int = 0,
        waitForTime: Long? = null,
        predicate: (String) -> Boolean
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, index = index, packageName = packageName).hasText(
                checkDescendants = checkDescendants,
                message = message,
                predicate = predicate
            )
        }
    }

    fun hasText(
        viewLookup: () -> TestView,
        checkDescendants: Boolean = true,
        message: String? = null,
        waitForTime: Long? = null,
        predicate: (String) -> Boolean
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().hasText(
                checkDescendants = checkDescendants,
                message = message,
                predicate = predicate
            )
        }
    }

    fun hasText(
        id: Int,
        text: Regex,
        checkDescendants: Boolean = true,
        index: Int = 0,
        childId: Int? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, childId = childId, index = index, packageName = packageName).hasText(
                text,
                checkDescendants = checkDescendants
            )
        }
    }

    fun hasText(
        text: String,
        exact: Boolean = false,
        index: Int = 0,
        ignoreCase: Boolean = false,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            if (ignoreCase) {
                val pattern = if (exact) Regex.escape(text) else ".*${Regex.escape(text)}.*"
                viewWithText(
                    Pattern.compile(pattern, Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
                    index = index
                )
            } else {
                viewWithText(text, index = index, contains = !exact)
            }
        }
    }

    fun hasText(
        viewLookup: () -> TestView,
        text: Regex,
        checkDescendants: Boolean = true,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().hasText(text, checkDescendants = checkDescendants)
        }
    }

    fun hasText(
        regex: Regex,
        index: Int = 0,
        exact: Boolean = false,
        waitForTime: Long? = null
    ) {
        val r = if (!exact) {
            Regex(".*${regex.pattern}.*", RegexOption.DOT_MATCHES_ALL)
        } else {
            regex
        }

        waitFor(resolveWaitForTime(waitForTime)) {
            viewWithText(r.toPattern(), index = index)
        }
    }

    /**
     * Verifies the texts of all of the visible views with the given ID, from top to bottom.
     */
    fun hasTextsInOrder(id: Int, texts: List<String>, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            assertEquals(texts, getTexts(id))
        }
    }

    /**
     * Verifies a data point (ex. a stat tile) is visible with the given value and label.
     */
    fun hasDataPoint(value: String, label: String, waitForTime: Long? = null) {
        hasDataPoint(Regex(Regex.escape(value)), label, waitForTime)
    }

    fun hasDataPoint(value: Regex, label: String, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            // A data point is a value and a label drawn together, so look for the value among the label's siblings
            val labels = TestUtils.device.findObjects(By.text(label))
            val dataPoints = labels.map { labelView ->
                val group = labelView.parent ?: labelView
                group.allTexts().filter { it != label } to label
            }
            assertTrue(
                "Expected data point $value / $label but was $dataPoints",
                dataPoints.any { (values, _) -> values.any { value.matches(it.trim()) } }
            )
        }
    }

    private fun getTexts(id: Int): List<String> {
        return TestUtils.device.findObjects(byResId(id, packageName)).map { it.text ?: "" }
    }

    /**
     * Verifies the texts of all of the visible Compose nodes with the given testTag, from top to bottom.
     */
    /**
     * Checks that every text is on screen and that they appear top to bottom in the given order. Compose rows have no
     * shared id to collect, so the order comes from where each text is drawn.
     */
    fun hasTextsInVerticalOrder(texts: List<String>, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            val tops = texts.map { text ->
                val node = TestUtils.device.findObject(By.text(text))
                    ?: throw AssertionError("Text not found: $text")
                node.visibleBounds.top
            }
            if (tops != tops.sorted()) {
                throw AssertionError("Texts out of order: ${texts.zip(tops)}")
            }
        }
    }

    fun hasTextsInOrder(tag: String, texts: List<String>, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            assertEquals(texts, getTexts(tag))
        }
    }

    private fun getTexts(tag: String): List<String> {
        val selector = By.res(packageName ?: TestUtils.context.packageName, tag)
        return TestUtils.device.findObjects(selector).map { it.allText() }
    }

    private fun UiObject2.allTexts(): List<String> {
        return listOfNotNull(text) + children.flatMap { it.allTexts() }
    }

    private fun UiObject2.allText(): String {
        text?.let { return it }
        return children.joinToString(" ") { it.allText() }.trim()
    }

    fun any(vararg actions: () -> Unit, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            var exception: Throwable? = null
            for (action in actions) {
                try {
                    action()
                    return@waitFor
                } catch (e: Throwable) {
                    exception = e
                }
            }
            if (exception != null) {
                throw exception
            }
        }
    }

    fun isChecked(
        id: Int,
        isChecked: Boolean = true,
        index: Int = 0,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, index = index, packageName = packageName).isChecked(isChecked)
        }
    }

    fun isChecked(
        viewLookup: () -> TestView,
        isChecked: Boolean = true,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().isChecked(isChecked)
        }
    }

    fun isChecked(
        viewText: String,
        isChecked: Boolean = true,
        index: Int = 0,
        exact: Boolean = false,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewWithText(viewText, !exact, index = index).isChecked(isChecked)
        }
    }

    fun isNotChecked(
        id: Int,
        index: Int = 0,
        waitForTime: Long? = null
    ) {
        isChecked(id, isChecked = false, index = index, waitForTime = waitForTime)
    }

    fun isNotChecked(
        viewLookup: () -> TestView,
        waitForTime: Long? = null
    ) {
        isChecked(viewLookup, isChecked = false, waitForTime = waitForTime)
    }

    fun isNotChecked(
        viewText: String,
        index: Int = 0,
        exact: Boolean = false,
        waitForTime: Long? = null
    ) {
        isChecked(
            viewText,
            isChecked = false,
            exact = exact,
            index = index,
            waitForTime = waitForTime
        )
    }

    fun string(@StringRes id: Int, vararg args: Any): String {
        return TestUtils.getString(id, *args)
    }

    /**
     * Runs the block, ignoring failures. Pass a [waitForTime] when the block is only a quick check
     * for something that is usually absent, otherwise every miss costs the default timeout.
     */
    fun optional(waitForTime: Long? = null, block: () -> Unit) {
        try {
            if (waitForTime == null) {
                block()
            } else {
                withWaitForTimeOverride(waitForTime, block)
            }
        } catch (_: Throwable) {
            // Do nothing
        }
    }

    /**
     * Dismisses a confirmation dialog which only appears in some states (ex. already navigating).
     */
    fun clickOkIfShown() {
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { clickOk() }
    }

    fun not(waitForTime: Long? = null, block: () -> Unit) {
        waitFor(resolveWaitForTime(waitForTime)) {
            withWaitForTimeOverride(0) {
                TestUtils.not { block() }
            }
        }
    }

    fun or(vararg blocks: () -> Unit) {
        var lastError: Throwable? = null
        for (block in blocks) {
            try {
                block()
                return
            } catch (e: Throwable) {
                lastError = e
            }
        }
        if (lastError != null) {
            throw lastError
        }
    }

    fun click(
        id: Int,
        index: Int = 0,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        childId: Int? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, childId = childId, index = index, packageName = packageName).click(
                holdDuration,
                xPercent,
                yPercent
            )
        }
    }

    fun click(
        view: TestView,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view.click(holdDuration, xPercent, yPercent)
        }
    }

    fun click(
        viewLookup: () -> TestView,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().click(holdDuration, xPercent, yPercent)
        }
    }

    fun click(
        text: String,
        index: Int = 0,
        holdDuration: Long? = null,
        exact: Boolean = false,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewWithText(text, index = index, contains = !exact).click(
                holdDuration,
                xPercent,
                yPercent
            )
        }
    }

    fun click(
        regex: Regex,
        index: Int = 0,
        holdDuration: Long? = null,
        xPercent: Float? = null,
        yPercent: Float? = null,
        waitForTime: Long? = null
    ) {
        click(
            { viewWithText(regex.toPattern(), index = index) },
            holdDuration,
            xPercent,
            yPercent,
            waitForTime
        )
    }

    fun longClick(
        id: Int,
        index: Int = 0,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, index = index, packageName = packageName).longClick()
        }
    }

    fun longClick(
        viewLookup: () -> TestView,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().longClick()
        }
    }

    fun longClick(
        text: String,
        index: Int = 0,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewWithText(text, index = index).longClick()
        }
    }

    fun longClick(
        view: TestView,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view.longClick()
        }
    }

    fun clickOk(index: Int = 0, waitForTime: Long? = null) {
        click(
            string(android.R.string.ok),
            index = index,
            waitForTime = waitForTime,
            exact = true
        )
    }

    fun input(
        view: TestView,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view.input(text, checkDescendants)
        }
    }

    fun input(
        viewLookup: () -> TestView,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long? = null,
        closeKeyboardOnCompletion: Boolean = false
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().input(text, checkDescendants)
        }
        if (closeKeyboardOnCompletion) {
            TestUtils.closeKeyboard()
        }
    }

    fun input(
        id: Int,
        text: String,
        checkDescendants: Boolean = true,
        waitForTime: Long? = null,
        closeKeyboardOnCompletion: Boolean = false
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, packageName = packageName).input(text, checkDescendants)
        }
        if (closeKeyboardOnCompletion) {
            TestUtils.closeKeyboard()
        }
    }

    fun input(
        viewText: String,
        text: String,
        checkDescendants: Boolean = true,
        exact: Boolean = false,
        isHint: Boolean = false,
        index: Int = 0,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            if (isHint) {
                viewWithHint(viewText, !exact, index = index)
            } else {
                viewWithText(viewText, !exact, index = index)
            }.input(text, checkDescendants)
        }
    }

    fun isNotVisible(id: Int, index: Int = 0, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            TestUtils.not { view(id, index = index, packageName = packageName) }
        }
    }

    fun isNotVisible(viewLookup: () -> TestView, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            TestUtils.not { viewLookup() }
        }
    }

    /**
     * Verifies a Compose node with the given testTag is visible.
     */
    fun isVisible(tag: String, index: Int = 0, waitForTime: Long? = null) {
        isVisible(id(tag, index), waitForTime)
    }

    fun isVisible(id: Int, index: Int = 0, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            view(id, index = index, packageName = packageName)
        }
    }

    fun isVisible(viewLookup: () -> TestView, waitForTime: Long? = null) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup()
        }
    }

    fun isTrue(waitForTime: Long? = null, predicate: () -> Boolean) {
        waitFor(resolveWaitForTime(waitForTime)) {
            assertTrue(predicate())
        }
    }

    fun isFalse(waitForTime: Long? = null, predicate: () -> Boolean) {
        waitFor(resolveWaitForTime(waitForTime)) {
            assertFalse(predicate())
        }
    }

    fun scrollToEnd(id: Int, index: Int = 0, waitForTime: Long? = null) {
        scrollToEnd({ view(id, index = index, packageName = packageName) }, waitForTime)
    }

    fun scrollToEnd(
        viewLookup: () -> TestView = { getScrollableView() },
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().scrollToEnd()
        }
    }

    fun scrollToStart(id: Int, index: Int = 0, waitForTime: Long? = null) {
        scrollToStart({ view(id, index = index, packageName = packageName) }, waitForTime)
    }

    fun scrollToStart(
        viewLookup: () -> TestView = { getScrollableView() },
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            viewLookup().scrollToEnd(Direction.UP)
        }
    }

    fun scrollUntil(
        id: Int,
        direction: Direction = Direction.DOWN,
        maxScrolls: Int = 10,
        amountPerScroll: Float = 0.5f,
        index: Int = 0,
        waitForTime: Long? = null,
        action: () -> Unit
    ) {
        scrollUntil(
            { view(id, index = index, packageName = packageName) },
            direction,
            maxScrolls,
            amountPerScroll,
            waitForTime,
            action
        )
    }

    fun delay(milliseconds: Long) {
        Thread.sleep(milliseconds)
    }

    fun scrollUntil(
        viewLookup: () -> TestView = { getScrollableView() },
        direction: Direction = Direction.DOWN,
        maxScrolls: Int = 10,
        amountPerScroll: Float = 0.5f,
        waitForTime: Long? = null,
        action: () -> Unit
    ) {
        var scrollsDone = 0
        var failedScrollCount = 0
        // The content may still be loading on the first attempt, but scrolling waits for it to settle
        var attemptTimeout = SCROLL_WAIT_FOR_TIMEOUT
        while (scrollsDone < maxScrolls) {
            try {
                withWaitForTimeOverride(attemptTimeout) {
                    action()
                }
                // Action succeeded, no need to scroll further
                return
            } catch (e: Throwable) {
                // Action failed, try scrolling
                var scrolled = false
                waitFor(resolveWaitForTime(waitForTime)) {
                    scrolled = viewLookup().scroll(direction, amountPerScroll)
                }
                if (!scrolled && failedScrollCount > 2) {
                    // Couldn't scroll further
                    break
                }

                if (!scrolled) {
                    failedScrollCount++
                } else {
                    failedScrollCount = 0
                    scrollsDone++
                    attemptTimeout = SCROLLED_WAIT_FOR_TIMEOUT
                }
            }
        }
        // Try action one last time after all scrolling
        withWaitForTimeOverride(SCROLL_WAIT_FOR_TIMEOUT) {
            action()
        }
    }

    fun backUntil(
        maxBacks: Int = 10,
        action: () -> Unit
    ) {
        var backsDone = 0
        while (backsDone < maxBacks) {
            try {
                withWaitForTimeOverride(BACK_WAIT_FOR_TIMEOUT) {
                    action()
                }
                // Action succeeded, no need to go back further
                return
            } catch (e: Throwable) {
                // Action failed, try going back
                TestUtils.back(false)
                backsDone++
            }
        }
        // Try action one last time after all backs
        withWaitForTimeOverride(BACK_WAIT_FOR_TIMEOUT) {
            action()
        }
    }

    fun hasNotification(
        id: Int,
        title: String? = null,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            val n = notification(id)
            if (title != null) {
                n.hasTitle(title)
            }
        }
    }

    fun doesNotHaveNotification(
        id: Int,
        waitForTime: Long? = null
    ) {
        waitFor(resolveWaitForTime(waitForTime)) {
            TestUtils.not { notification(id) }
        }
    }

    const val BACK_WAIT_FOR_TIMEOUT = 1000L
    const val SCROLL_WAIT_FOR_TIMEOUT = 1000L
    const val SCROLLED_WAIT_FOR_TIMEOUT = 300L
    const val OPTIONAL_WAIT_FOR_TIMEOUT = 2000L
    const val DEFAULT_WAIT_FOR_TIMEOUT = 7000L
    const val GPS_WAIT_FOR_TIMEOUT = 35000L
}
