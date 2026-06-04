package com.kylecorry.trail_sense.tools.paths

import androidx.test.uiautomator.Direction
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test


class ToolPathsTest : ToolTestBase(Tools.PATHS) {
    @Test
    fun verifyBasicFunctionality() {
        hasText(id("paths_title"), string(R.string.paths))

        canUseBacktrack()
        canRenamePath()
        canViewPathDetails()
        addPathGroup()
        importPath()
        createEmptyPath()
        groupOperations()
        pathOperations()
        searchPath()
        changeSort()
        // TODO: Quick settings tile
        verifyQuickAction()
    }

    private fun addPathGroup() {
        click(id("add_btn"))
        click(string(R.string.group))
        input(string(R.string.name), "Test Group")
        clickOk()

        hasText("Test Group")
        click("Test Group")
        hasText(string(R.string.no_paths))
        back(false)
    }

    private fun importPath() {
        // Open import and verify the picker opens, then back out
        click(id("add_btn"))
        click(string(R.string.import_gpx))
        optional { hasText(string(R.string.pick_file)) }
        back(false)
        hasText(id("paths_title"), string(R.string.paths))
    }

    private fun createEmptyPath() {
        click(id("add_btn"))
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Empty Path")
        clickOk()
        isVisible(R.id.path_title)
        hasText("Empty Path")
        backUntil { isVisible(id("paths_title")) }
    }

    private fun groupOperations() {
        // Ensure there is a second group to move into
        click(id("add_btn"))
        click(string(R.string.group), exact = true)
        input(string(R.string.name), "Dest Group")
        clickOk()
        hasText("Dest Group")

        // Rename group "Test Group" -> "Test Group 2"
        hasText("Test Group")
        click(id("path_group_menu_Test Group"))
        click(string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")

        // Export current group via toolbar menu
        click(id("paths_menu_btn"))
        click(string(R.string.export))
        clickOk()
        optional { hasText("trail-sense-") }
        backUntil { isVisible(id("paths_title")) }

        // Move group into Dest Group
        hasText("Test Group 2")
        click(id("path_group_menu_Test Group 2"))
        click(string(R.string.move_to))
        click("Dest Group")
        click(string(R.string.move))
        optional { hasText(string(R.string.moved_to, "Dest Group")) }

        // Delete moved group (open Dest Group first)
        click("Dest Group")
        hasText("Test Group 2")
        click(id("path_group_menu_Test Group 2"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
        back(false)
    }

    private fun pathOperations() {
        // Create another path
        click(id("add_btn"))
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Second Path")
        clickOk()
        hasText("Second Path")
        backUntil { isVisible(id("paths_title")) }

        // Rename
        click(id("path_item_menu_Second Path"))
        click(string(R.string.rename))
        input("Second Path", "Empty Path 2")
        clickOk()
        hasText("Empty Path 2")

        // Hide / Show
        click(id("path_visibility_toggle_Empty Path 2"))
        click(id("path_visibility_toggle_Empty Path 2"))

        // Export
        click(id("path_item_menu_Empty Path 2"))
        click(string(R.string.export))
        optional { hasText("trail-sense-") }
        backUntil { isVisible(id("paths_title")) }

        // Merge "Empty Path 2" into "Empty Path"
        click(id("path_item_menu_Empty Path 2"))
        click(string(R.string.merge))
        click("Empty Path", exact = true)
        optional { hasText(string(R.string.merging)) }
        not { hasText("Empty Path 2") }

        // Simplify path
        click(id("path_item_menu_Empty Path"))
        click(string(R.string.simplify))
        click("High")
        clickOk()

        // Move Empty Path to Dest Group
        click(id("path_item_menu_Empty Path"))
        click(string(R.string.move_to))
        click("Dest Group")
        click(string(R.string.move))

        // Delete path
        click(id("path_item_menu_Empty Path"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Empty Path 2") }
    }

    private fun searchPath() {
        input(id("searchbox")(), "Path")
        hasText("Empty Path")
        not { hasText("Dest Group") }
        input(id("searchbox")(), "")
        hasText("Dest Group")
    }

    private fun changeSort() {
        click(id("paths_menu_btn"))
        click(string(R.string.sort_by, string(R.string.most_recent)))
        click(string(R.string.name))
        clickOk()
        click(id("paths_menu_btn"))
        hasText(string(R.string.sort_by, string(R.string.name)))
        back(false)
    }

    private fun canViewPathDetails() {
        // Open the path
        click("Test Path")

        // Wait for the path to open
        hasText(R.id.path_title, "Test Path")

        // Settings
        click(R.id.path_color)
        clickOk()

        click("Dotted")
        hasOptions("Solid", "Dotted", "Arrow", "Dashed", "Square", "Diamond", "Cross")
        click("Solid")
        clickOk()
        hasText("Solid")

        changePointStyle("None", "Cell signal")
        changePointStyle("Cell signal", "Elevation")
        changePointStyle("Elevation", "Time")
        changePointStyle("Time", "Slope")
        changePointStyle("Slope", "None")

        // Stats
        hasText("0m")
        hasText("Duration")

        hasText("1")
        hasText("Points")

        hasText("0 ft")
        hasText("Distance")

        hasText("Easy")
        hasText("Difficulty")

        scrollUntil(R.id.path_scroll) {
            hasText("0 ft")
            hasText("Ascent")
        }

        hasText("0 ft")
        hasText("Descent")

        scrollUntil(R.id.path_scroll) {
            hasText(Regex("\\d+ ft"))
            hasText("Lowest point")
        }

        hasText(Regex("\\d+ ft"))
        hasText("Highest point")

        scrollUntil(R.id.path_scroll) {
            isVisible(R.id.chart)
        }

        // Add a point
        scrollUntil(R.id.path_scroll) {
            click(R.id.add_point_btn)
        }

        not(waitForTime = GPS_WAIT_FOR_TIMEOUT) { hasText("Loading") }

        scrollUntil(R.id.path_scroll, direction = Direction.UP) {
            hasText("2")
        }

        // Navigate
        scrollUntil(R.id.path_scroll) {
            click("Navigate")
        }

        clickOk()
        isVisible(R.id.navigation_title)
        hasText("Test Path")
        back()
        back()
        click("Test Path")

        // Path points
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Points")
        hasText("Today")

        // TODO: This isn't working on the emulator
        back()

        // Simplify
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Simplify")
        hasOptions("High", "Moderate", "Low")
        click("High")
        clickOk()

        // Export
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Export")
        hasText("trail-sense-")
        backUntil {
            isVisible(R.id.path_title)
        }

        // Hide / show
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Hide")
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Show")

        // Keep forever
        click(toolbarButton(R.id.path_title, Side.Right))
        click("Keep forever")
        click(toolbarButton(R.id.path_title, Side.Right))
        not { hasText("Keep forever") }

        backUntil { isVisible(id("paths_title")) }
    }

    private fun changePointStyle(previousStyle: String, newStyle: String) {
        click(previousStyle)
        click(newStyle)
        clickOk()
        hasText(newStyle)
    }

    private fun hasOptions(vararg options: String) {
        for (option in options) {
            scrollUntil {
                hasText(option)
            }
        }
        optional {
            scrollToStart()
        }
    }

    private fun canRenamePath() {
        click(id("path_item_menu_Today"))
        click(string(R.string.rename))
        input(string(R.string.name), "Test Path")
        clickOk()
        hasText("Test Path")
    }

    private fun canUseBacktrack() {
        // Verify it will run every 15 minutes by default
        hasText(id("play_bar_title"), "Off - 15m")

        // Click the start button
        click(id("play_btn"))


        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            }
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        hasText(id("play_bar_title"), "On - 15m")

        // Wait for the path to be created
        hasText("Today", waitForTime = GPS_WAIT_FOR_TIMEOUT)

        // Stop backtrack
        click(id("play_btn"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(BacktrackAlerter.NOTIFICATION_ID).hasTitle(R.string.backtrack)
            }
        }

        // Wait for the path to be created
        hasText("Today", waitForTime = GPS_WAIT_FOR_TIMEOUT)

        click(quickAction(Tools.QUICK_ACTION_BACKTRACK))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }

        TestUtils.closeQuickActions()
    }
}
