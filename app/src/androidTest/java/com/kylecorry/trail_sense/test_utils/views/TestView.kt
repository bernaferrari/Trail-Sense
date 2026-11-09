package com.kylecorry.trail_sense.test_utils.views

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.device
import com.kylecorry.trail_sense.test_utils.byResId
import com.kylecorry.trail_sense.test_utils.byResName
import java.util.regex.Pattern

class TestView(val uiObject: UiObject2)

fun view(
    @IdRes id: Int,
    @IdRes childId: Int? = null,
    index: Int = 0,
    packageName: String? = null
): TestView {
    val selector = if (childId == null) {
        byResId(id, packageName)
    } else {
        byResId(childId, packageName).hasAncestor(byResId(id, packageName))
    }
    return view(selector, index)
}

fun viewWithResourceId(
    id: String,
    index: Int = 0
): TestView {
    return view(By.res(id), index)
}

/**
 * Looks up a Compose testTag or view id by entry name, for example `id("play_btn")`.
 * The returned lookup is what [com.kylecorry.trail_sense.test_utils.AutomationLibrary] waits on.
 * @param index which match to use when several nodes share the tag (top to bottom)
 */
fun id(resourceName: String, index: Int = 0): () -> TestView = {
    view(byResName(resourceName), index)
}

/**
 * Looks up the [index]th clickable node inside the node tagged [resourceName], such as a row in a tagged list group.
 */
fun clickableIn(resourceName: String, index: Int = 0): () -> TestView = {
    view(By.clickable(true).hasAncestor(byResName(resourceName)), index)
}

/**
 * Looks up a node by its content description, which is how Compose icon buttons and custom controls are named.
 */
fun desc(description: String, contains: Boolean = false, index: Int = 0): () -> TestView = {
    view(if (contains) By.descContains(description) else By.desc(description), index)
}

fun viewWithText(
    text: String,
    contains: Boolean = false,
    parentId: Int? = null,
    index: Int = 0
): TestView {
    return view(if (contains) By.textContains(text) else By.text(text), index)
}

fun viewWithHint(
    text: String,
    contains: Boolean = false,
    index: Int = 0
): TestView {
    return view(if (contains) By.hintContains(text) else By.hint(text), index)
}

fun viewWithText(text: Pattern, index: Int = 0): TestView {
    return view(By.text(text), index)
}

fun viewWithText(@StringRes text: Int, contains: Boolean = false, index: Int = 0): TestView {
    return viewWithText(TestUtils.getString(text), contains, index = index)
}

fun getScrollableView(index: Int = 0): TestView {
    return view(By.scrollable(true), index)
}

fun view(selector: BySelector, index: Int = 0): TestView {
    val obj = device.findObjects(selector).getOrNull(index)
        ?: throw AssertionError("View not found: $selector, index: $index")
    return TestView(obj)
}

fun TestView.childWithIndex(index: Int = 0): TestView {
    return TestView(uiObject.children[index])
}

/**
 * Looks up the radio row reading [text] in an options dialog, so it is not confused with the field or pill
 * behind the dialog that shows the current choice.
 */
fun option(text: String): () -> TestView = {
    val merged = By.checkable(true).text(text)
    if (device.hasObject(merged)) {
        view(merged)
    } else {
        view(By.checkable(true).hasDescendant(By.text(text)))
    }
}

/**
 * Looks up a Compose text field by its label. Sheets and dialogs are drawn in their own windows, which do not
 * expose test tags as resource ids, so fields there are found by the label drawn inside them.
 */
fun field(label: String, index: Int = 0): () -> TestView = {
    view(By.clazz("android.widget.EditText").hasDescendant(By.text(label)), index)
}
