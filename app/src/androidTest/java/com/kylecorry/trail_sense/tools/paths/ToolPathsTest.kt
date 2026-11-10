package com.kylecorry.trail_sense.tools.paths

import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.test_utils.views.view
import java.util.regex.Pattern
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInVerticalOrder
import android.net.Uri
import androidx.test.uiautomator.Direction
import com.kylecorry.luna.result.Result
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.GpxIOService
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriPickerError
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.OPTIONAL_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRoute
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.infrastructure.PathNavigationStore
import com.kylecorry.trail_sense.tools.navigation.infrastructure.PathNavigator
import com.kylecorry.trail_sense.tools.paths.domain.FullPath
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class ToolPathsTest : ToolTestBase(Tools.PATHS, Coordinate(42.03, -71.97)) {

    // Each navigator listens for location changes for as long as the process lives and saves its own
    // session when one arrives, so one left over from an earlier test would overwrite this test's session
    private val navigators = mutableListOf<PathNavigator>()

    private fun newNavigator(): PathNavigator {
        return PathNavigator(TestUtils.context).also { navigators.add(it) }
    }

    @After
    fun cancelNavigators() {
        navigators.forEach { it.cancel() }
        // The app's navigator lives as long as the test process, so a path followed through the UI would keep saving
        // its progress into the next test
        runBlocking { Navigator.getInstance(TestUtils.context).cancelAllNavigation() }
    }
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
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.pick_file)) }
        back(false)
        hasText(id("paths_title"), string(R.string.paths))
    }

    private fun createEmptyPath() {
        click(id("add_btn"))
        click(string(R.string.path), exact = true)
        input(string(R.string.name), "Empty Path")
        clickOk()
        isVisible(id("path_menu_btn"))
        hasText("Empty Path")
        backUntil { isVisible(id("paths_title")) }
    }

    @Test
    fun canCreatePathOnMap() {
        // The map only draws (and so only starts a path) when there is a visible offline map
        relaunchTool { TestData.addOfflineMap("Test Map") }
        click(R.id.add_btn)
        click(string(R.string.create_path_on_map))
        clickOkIfShown()
        hasText("Distance")
        hasText("Create path")

        // Tapping the map should add points to the path
        click(R.id.map, xPercent = 0.3f, yPercent = 0.3f)
        // Wait so the next tap isn't treated as a double tap (zoom)
        delay(600)
        click(R.id.map, xPercent = 0.7f, yPercent = 0.3f)
        hasText(Regex("[1-9]\\d*(\\.\\d+)? (mi|ft)"))

        // Cancelling a path with points asks before discarding it. The distance sheet has its own cancel button; the
        // top bar's right action is the map's menu.
        click(desc(string(android.R.string.cancel)))
        click(string(R.string.dialog_leave))
        backUntil { isVisible(R.id.paths_title) }
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
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.moved_to, "Dest Group")) }

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
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) { hasText(string(R.string.merging)) }
        not { hasText("Empty Path 2") }

        // Simplify path
        click(id("path_item_menu_Empty Path"))
        click(string(R.string.simplify))
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
        input(id("searchbox"), "Path")
        hasText("Empty Path")
        not { hasText("Dest Group") }
        input(id("searchbox"), "")
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
        isVisible(id("path_menu_btn"))
        hasText("Test Path", exact = true)

        // Settings
        scrollUntil { click(R.id.path_color) }
        clickOk()

        // The line style row can be below the fold under the color row
        scrollUntil { click("Dotted") }
        hasOptions("Solid", "Dotted", "Arrow", "Dashed", "Square", "Diamond", "Cross")
        // Single-choice pickers apply the tapped option and close; there is no OK
        click("Solid")
        clickOkIfShown()
        hasText("Solid")

        changePointStyle("None", "Elevation")
        changePointStyle("Elevation", "None")

        // Stats
        hasText("0m")
        hasText("Duration")

        hasText("0 ft")
        hasText("Distance")

        scrollUntil {
            hasText("1")
            hasText("Points")
            hasText("Easy")
            hasText("Difficulty")
        }

        scrollUntil {
            hasText("0 ft")
            hasText("Ascent")
        }

        hasText("0 ft")
        hasText("Descent")

        scrollUntil {
            hasText(Regex("\\d+ ft"))
            hasText("Lowest point")
        }

        hasText(Regex("\\d+ ft"))
        hasText("Highest point")

        scrollUntil {
            isVisible(R.id.chart)
        }

        // Add a point away from the first so the path can be followed
        TestUtils.setLocationOverride(Coordinate(42.04, -71.97))
        scrollUntil {
            click(R.id.add_point_btn)
        }

        not(waitForTime = GPS_WAIT_FOR_TIMEOUT) { hasText("Loading") }

        scrollUntil(direction = Direction.UP) {
            hasText("2")
        }

        // Navigate
        scrollUntil { click(string(R.string.path_direction_to_end), exact = true) }

        // The navigation card, which has no tags, is marked by its stop button
        isVisible(desc(string(R.string.stop)))
        hasText("Test Path")
        back()
        back()
        click("Test Path")

        // Path points
        click(toolbarButton(Side.Right))
        click("Points")
        hasText("Today")

        // TODO: This isn't working on the emulator
        back()

        // Replace elevations
        click(toolbarButton(Side.Right))
        click(string(R.string.replace_elevations))
        hasText(string(R.string.replace_elevations_confirmation))
        back(false)

        // Simplify
        click(toolbarButton(Side.Right))
        click("Simplify")
        hasText(string(R.string.quality))
        hasText("High", exact = true)
        hasText("Moderate", exact = true)
        click("Low", exact = true)
        hasText("will be deleted")
        clickOk()

        // Export
        click(toolbarButton(Side.Right))
        click("Export")
        hasText("trail-sense-")
        backUntil {
            isVisible(id("path_menu_btn"))
        }

        // Hide / show
        click(toolbarButton(Side.Right))
        click("Hide")
        click(toolbarButton(Side.Right))
        click("Show")

        backUntil { isVisible(id("paths_title")) }
    }

    private fun changePointStyle(previousStyle: String, newStyle: String) {
        scrollUntil { click(previousStyle) }
        click(newStyle)
        clickOkIfShown()
        hasText(newStyle)
    }

    private fun hasOptions(vararg options: String) {
        for (option in options) {
            scrollUntil {
                hasText(option)
            }
        }
        optional(OPTIONAL_WAIT_FOR_TIMEOUT) {
            scrollToStart()
        }
    }

    private fun canRenamePath() {
        // The recorded path is named after its time span, so match the tag's prefix
        click({ view(By.res(Pattern.compile("(.*:id/)?path_item_menu_Today.*"))) })
        click(string(R.string.rename))
        input(string(R.string.name), "Test Path")
        clickOk()
        hasText("Test Path")
    }

    private fun canUseBacktrack() {
        // Backtrack starts off; the switch row names its state
        hasText(id("play_bar_title"), string(R.string.places_backtrack_off_title))

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

        // It records every minute by default
        hasText(id("play_bar_title"), string(R.string.places_backtrack_on_title))
        hasText(string(R.string.places_backtrack_every, "1m"))

        // Wait for the path to be created
        hasText("Today", waitForTime = GPS_WAIT_FOR_TIMEOUT)

        hasText(string(R.string.temporary))

        // Stop backtrack
        click(id("play_btn"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(BacktrackAlerter.NOTIFICATION_ID) }
        }

        // The path is kept when stopped
        not { hasText(string(R.string.temporary)) }
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

    // Uses real recorded hikes (androidTest/assets/paths) so the numbers shown can be checked against
    // the GPX files themselves.

    private fun openWithSavedPaths() {
        assumeFalse(isStagingBuild)
        relaunchTool { seedPaths() }
    }

    private fun seedPaths() {
        TestData.addPathFromGpx("Sprague", "paths/sprague.gpx")
        TestData.addPathFromGpx("Durfee Short", "paths/durfee short.gpx")
        TestData.addPathFromGpx("Mansfield", "paths/mount mansfield.gpx")
    }

    @Test
    fun canKeepATemporaryPathForever() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addPath(
                "Recording",
                TestData.loadGpxPoints("paths/durfee short.gpx"),
                temporary = true
            )
        }

        hasText(string(R.string.temporary))
        click("Recording")
        isVisible(id("path_menu_btn"))
        hasText("Recording", exact = true)

        click(toolbarButton(Side.Right))
        click(string(R.string.keep_forever))
        click(toolbarButton(Side.Right))
        not { hasText(string(R.string.keep_forever)) }
    }

    @Test
    fun canStopBacktrackFromRecordingPathDetails() {
        assumeFalse(isStagingBuild)

        click(R.id.play_btn)
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }
        hasText(id("play_bar_title"), string(R.string.places_backtrack_on_title))
        hasText(string(R.string.places_backtrack_every, "1m"))

        // Wait for the path to be created
        // The backtrack bar is the first clickable in the list, so look for the recorded path's row by its
        // point count, which ends a subtitle such as "Temporary · 1m · 3 points"
        val recordedPath = {
            view(By.clickable(true).hasDescendant(By.text(Pattern.compile(".*\\b\\d+ points?$"))))
        }
        isVisible(recordedPath, waitForTime = GPS_WAIT_FOR_TIMEOUT)

        click(recordedPath)
        isVisible(id("stop_recording_btn"))

        click(id("stop_recording_btn"))
        not { isVisible(id("stop_recording_btn")) }

        back()
        hasText(id("play_bar_title"), string(R.string.places_backtrack_off_title))
    }

    @Test
    fun listShowsEachPathWithItsLength() {
        openWithSavedPaths()

        hasText(R.id.paths_title, string(R.string.paths))
        hasTitlesAndLengths(
            "Mansfield" to "4.91 mi",
            "Durfee Short" to "0.5 mi",
            "Sprague" to "3.71 mi"
        )
    }

    @Test
    fun canSortPaths() {
        openWithSavedPaths()

        sortBy(string(R.string.most_recent), string(R.string.name))
        hasTitlesInOrder("Durfee Short", "Mansfield", "Sprague")

        sortBy(string(R.string.name), string(R.string.shortest))
        hasTitlesInOrder("Durfee Short", "Sprague", "Mansfield")

        sortBy(string(R.string.shortest), string(R.string.longest))
        hasTitlesInOrder("Mansfield", "Sprague", "Durfee Short")
    }

    @Test
    fun canSearchPaths() {
        openWithSavedPaths()

        input(R.id.searchbox, "spra")
        hasText("Sprague")
        not { hasText("Mansfield", waitForTime = 0) }
        not { hasText("Durfee Short", waitForTime = 0) }

        input(R.id.searchbox, "")
        hasText("Mansfield")
        hasText("Durfee Short")
        hasText("Sprague")
    }

    @Test
    fun pathDetailsShowTheRecordedStatistics() {
        openWithSavedPaths()

        click("Durfee Short")
        isVisible(id("path_menu_btn"))
        hasText("Durfee Short", exact = true)

        // Recorded from 17:20:52 to 17:36:23
        hasDataPoint("15m", string(R.string.duration))
        // 0.498 miles between the 39 recorded points
        hasDataPoint("0.5 mi", string(R.string.distance))
        scrollUntil {
            hasDataPoint("39", string(R.string.points))
            hasDataPoint("Moderate", string(R.string.difficulty))
            hasDataPoint(Regex("\\d{1,2} ft"), string(R.string.ascent))
            hasDataPoint(Regex("-\\d{2} ft"), string(R.string.descent))
        }

        // The recorded elevations range from 522 ft to 590 ft
        scrollUntil {
            hasDataPoint(Regex("5[2-4]\\d ft"), string(R.string.lowest_point_elevation))
        }
        // The add point button can cover the value until the row is scrolled above it
        scrollUntil {
            hasDataPoint(Regex("5[7-9]\\d ft"), string(R.string.highest_point_elevation))
        }
        isVisible(R.id.chart)
    }

    @Test
    fun durationAndClimbAreEstimatedFromRealRecordings() {
        openWithSavedPaths()

        click("Sprague")
        isVisible(id("path_menu_btn"))
        hasText("Sprague", exact = true)
        hasDataPoint("3.71 mi", string(R.string.distance))
        // The recording has no meaningful timestamps, so the duration is estimated from the pace
        hasDataPoint(Regex("1h \\d+m"), string(R.string.duration))
        scrollUntil {
            hasDataPoint("90", string(R.string.points))
        }
        back()

        click("Mansfield")
        isVisible(id("path_menu_btn"))
        hasText("Mansfield", exact = true)
        hasDataPoint("4.91 mi", string(R.string.distance))
        // The raw recording climbs 2655 ft, the app smooths out elevation noise
        scrollUntil {
            hasDataPoint("175", string(R.string.points))
            hasDataPoint(Regex("25\\d\\d ft"), string(R.string.ascent))
            hasDataPoint(Regex("-25\\d\\d ft"), string(R.string.descent))
        }
    }

    @Test
    fun canFollowAPathInEitherDirection() {
        openWithSavedPaths()

        click("Durfee Short")
        // The path overview is a Compose page without tags; its menu button marks it, and the navigate buttons are
        // further down the page
        scrollUntil { hasText(string(R.string.path_direction_to_start), exact = true) }
        scrollUntil { click(string(R.string.path_direction_to_end), exact = true) }

        // The navigation card, which has no tags, is marked by its stop button
        isVisible(desc(string(R.string.stop)))
        assertFalse(PathNavigationStore().load()!!.reversed)
        hasText("Durfee Short", exact = true)
        isVisible({ view(By.clazz("android.widget.ProgressBar")) })
        // The route's distance is covered by PathRouteBuilderTest
        hasText(Regex("\\d+(\\.\\d+)?( mi)?"), exact = true)
        isVisible(desc(string(R.string.stop)))

        // Following a different direction replaces the route
        backUntil { isVisible(id("path_menu_btn")) }
        scrollUntil { click(string(R.string.path_direction_to_start), exact = true) }

        // The navigation card, which has no tags, is marked by its stop button
        isVisible(desc(string(R.string.stop)))
        assertTrue(PathNavigationStore().load()!!.reversed)
    }

    @Test
    fun aPathWithNoLengthCannotBeFollowed() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addPath("Single Point", listOf(PathPoint(0, 0, Coordinate(42.0, -72.0), 0f)))
        }

        click("Single Point")

        isVisible(id("path_menu_btn"))
        not { hasText(string(R.string.path_direction_to_end), exact = true) }
        assertNull(PathNavigationStore().load())
    }

    @Test
    fun canNavigateToAWaypointAlongThePathOrDirectly() {
        openWithSavedPaths()

        click("Durfee Short")
        click(toolbarButton(Side.Right))
        click("Points")
        // Each point's menu button is read out as "Details"
        click(desc(string(R.string.details)))
        click(string(R.string.path_navigation_follow_to_point))

        // The navigation card, which has no tags, is marked by its stop button
        isVisible(desc(string(R.string.stop)))
        assertNotNull(PathNavigationStore().load()!!.destinationPointId)

        backUntil { isVisible(id("path_menu_btn")) }
        click(toolbarButton(Side.Right))
        click("Points")
        click(desc(string(R.string.details)))
        click(string(R.string.path_navigation_direct))

        val navigator = Navigator.getInstance(TestUtils.context)
        waitFor { assertTrue(runBlocking { navigator.getDestination() } is Destination.Beacon) }
        assertNull(PathNavigationStore().load())
    }

    @Test
    fun canDeleteAPath() {
        openWithSavedPaths()

        hasText("Durfee Short")
        // The list is ordered by most recent: Mansfield, Durfee Short, Sprague
        clickListItemMenu(string(R.string.delete), menu = id("path_item_menu_Durfee Short"))
        clickOk()

        not { hasText("Durfee Short") }
        hasText("Mansfield")
        hasText("Sprague")
    }

    private fun sortBy(current: String, new: String) {
        click(toolbarButton(Side.Right))
        click(string(R.string.sort_by, current))
        click(new)
        // Picking a sort applies it
        clickOkIfShown()
    }

    private fun hasTitlesInOrder(vararg titles: String) {
        hasTextsInVerticalOrder(titles.toList())
    }

    private fun hasTitlesAndLengths(vararg paths: Pair<String, String>) {
        hasTitlesInOrder(*paths.map { it.first }.toTypedArray())
        hasTextsInVerticalOrder(paths.map { it.second })
    }

    // Exports saved paths to GPX and imports the result, to verify nothing is lost in the file.

    private val uri = Uri.parse("content://test/paths.gpx")
    private val files = InMemoryUriService()
    private val service = GpxIOService(InMemoryUriPicker(uri), files)

    @Test
    fun exportedPathsCanBeImportedWithoutLosingData() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val groupId = TestData.addPathGroup("Hikes")
        val pathId = TestData.addPathFromGpx("Durfee", "paths/durfee short.gpx", groupId)
        val original = pathService.getWaypoints(pathId)
        val fullPath = FullPath(
            pathService.getPath(pathId)!!,
            original,
            pathService.getGroup(groupId)
        )

        assertTrue(service.export(PathGPXConverter().toGPX(listOf(fullPath)), "paths.gpx"))
        val imported = service.import()

        assertNotNull(imported)
        assertEquals(1, imported!!.tracks.size)
        val track = imported.tracks[0]
        assertEquals("Durfee", track.name)
        assertEquals("Hikes", track.group)
        val points = track.segments.single().points
        assertEquals(39, points.size)
        for ((expected, actual) in original.zip(points)) {
            assertEquals(expected.coordinate.latitude, actual.coordinate.latitude, 0.00001)
            assertEquals(expected.coordinate.longitude, actual.coordinate.longitude, 0.00001)
            assertEquals(expected.elevation!!, actual.elevation!!, 0.01f)
            assertEquals(expected.time, actual.time)
        }
    }

    @Test
    fun exportedFileIsGpx() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val pathId = TestData.addPathFromGpx("Durfee", "paths/durfee short.gpx")
        val fullPath = FullPath(
            pathService.getPath(pathId)!!,
            pathService.getWaypoints(pathId)
        )

        service.export(PathGPXConverter().toGPX(listOf(fullPath)), "paths.gpx")

        val text = files.text!!
        assertTrue(text.contains("<gpx"))
        assertTrue(text.contains("creator=\"Trail Sense\""))
        assertEquals(39, Regex("<trkpt ").findAll(text).count())
    }

    @Test
    fun multiplePathsAreExportedAsSeparateTracks() = runBlocking {
        assumeFalse(isStagingBuild)
        val pathService = PathService.getInstance(TestUtils.context)
        val paths = listOf("Durfee" to "paths/durfee short.gpx", "Sprague" to "paths/sprague.gpx")
            .map { (name, asset) ->
                val id = TestData.addPathFromGpx(name, asset)
                FullPath(pathService.getPath(id)!!, pathService.getWaypoints(id))
            }

        service.export(PathGPXConverter().toGPX(paths), "paths.gpx")
        val imported = service.import()!!

        assertEquals(listOf("Durfee", "Sprague"), imported.tracks.map { it.name })
        assertEquals(listOf(39, 90), imported.tracks.map { it.segments.single().points.size })
    }

    // PATH NAVIGATION
    // These use the navigator directly, with real hikes (androidTest/assets/paths), the database,
    // and the saved navigation state. The routing logic itself is covered by PathRouteTest.
    //
    // Durfee Short is a loop (its start and end are 9 m apart) and Durfee Out is an out and back
    // path. The expected distances were calculated from the GPX files.

    // The point 198 m along the loop, there is no other part of the loop nearby

    @Test
    fun navigationIsRestoredWithItsProgress() = runBlocking {
        val points = coordinatesOf(OUT_AND_BACK)
        val route = follow(OUT_AND_BACK, reversed = false, from = points[0])
        val location = Coordinate(points[28].latitude + 0.00001, points[28].longitude)
        route.navigate(points[28])
        // The navigator saves the progress when the location changes
        UserPreferences(TestUtils.context).gps.locationOverride = location
        Tools.broadcast(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
        waitFor { assertNotNull(PathNavigationStore().load()?.progress) }

        // A new navigator is what the app has after it is restarted
        val restored = newNavigator()
        restored.awaitRestore()
        val destination = restored.destination.first()

        assertNotNull(destination)
        assertEquals("Durfee Out", destination!!.path.name)
        // Without the saved progress the route would start over and need to find the shortcut again
        assertEquals(663.6f, destination.route.navigate(location).remainingDistance.meters().value, 3f)
    }

    @Test
    fun navigatingToAnotherPathReplacesThePreviousOneUntilItIsStopped() = runBlocking {
        val navigator = newNavigator()
        follow(OUT_AND_BACK, reversed = false, from = coordinatesOf(OUT_AND_BACK)[0], navigator)
        assertEquals("Durfee Out", navigator.destination.first()!!.path.name)

        follow(LOOP, reversed = false, from = coordinatesOf(LOOP)[0], navigator)

        assertEquals("Durfee Short", navigator.destination.first()!!.path.name)
        val restored = newNavigator()
        restored.awaitRestore()
        assertEquals("Durfee Short", restored.destination.first()!!.path.name)

        navigator.cancel()

        assertNull(navigator.destination.first())
        val restoredAfterStopping = newNavigator()
        restoredAfterStopping.awaitRestore()
        assertNull(restoredAfterStopping.destination.first())
    }

    @Test
    fun restoredNavigationUsesTheElevationSmoothingSetting() = runBlocking {
        val prefs = UserPreferences(TestUtils.context).paths
        val start = coordinatesOf(OUT_AND_BACK)[0]

        val defaultGain = follow(OUT_AND_BACK, reversed = false, from = start)
            .navigate(start).remainingElevationGain.meters().value
        PreferencesSubsystem.getInstance(TestUtils.context).preferences
            .putInt(TestUtils.context.getString(R.string.pref_elevation_smoothing), 1000)
        val restored = newNavigator()
        restored.awaitRestore()
        val smoothedGain = restored.destination.first()!!.route
            .navigate(start).remainingElevationGain.meters().value

        assertEquals(1f, prefs.elevationSmoothing, 0.001f)
        assertTrue(smoothedGain < defaultGain)
    }

    @Test
    fun findsTheMostRecentPathWithEnoughElevationChange() = runBlocking {
        val pathService = PathService.getInstance(TestUtils.context)
        assertNull(pathService.getMostRecentPathIdWithElevationChange(50f))

        val hilly = TestData.addPathFromGpx("Hilly", "paths/presidential range.gpx")
        TestData.addPathFromGpx("Flat", "paths/durfee short.gpx")

        assertEquals(hilly, pathService.getMostRecentPathIdWithElevationChange(50f))
        assertNull(pathService.getMostRecentPathIdWithElevationChange(5000f))
    }

    /**
     * Saves the path and starts following it from the location.
     */
    private suspend fun follow(
        asset: String,
        reversed: Boolean,
        from: Coordinate,
        navigator: PathNavigator = newNavigator()
    ): PathRoute {
        assumeFalse(isStagingBuild)
        UserPreferences(TestUtils.context).gps.locationOverride = from
        val name = if (asset == LOOP) "Durfee Short" else "Durfee Out"
        val pathService = PathService.getInstance(TestUtils.context)
        val id = TestData.addPathFromGpx(name, asset)
        val direction = if (reversed) PathDirection.ToStart else PathDirection.ToEnd
        navigator.navigate(pathService.getPath(id)!!, PathFollow.Direction(direction))
        navigator.awaitRestore()
        return navigator.destination.first()!!.route
    }

    private fun PathRoute.navigate(location: Coordinate) = navigate(location, null)

    private fun coordinatesOf(asset: String): List<Coordinate> {
        return TestData.loadGpxPoints(asset).map { it.coordinate }
    }

    private class InMemoryUriPicker(private val uri: Uri) : UriPicker {
        override suspend fun open(
            types: List<String>,
            requirePersistentAccess: Boolean
        ): Result<Uri, UriPickerError> = Result.Ok(uri)

        override suspend fun create(filename: String, type: String): Uri = uri
    }

    private class InMemoryUriService : UriService {
        var text: String? = null

        override suspend fun write(uri: Uri, data: String): Boolean {
            text = data
            return true
        }

        override suspend fun outputStream(uri: Uri): OutputStream = ByteArrayOutputStream()

        override suspend fun read(uri: Uri): String? = text

        override suspend fun inputStream(uri: Uri): InputStream? {
            return text?.let { ByteArrayInputStream(it.toByteArray()) }
        }
    }

    companion object {
        // A loop (the start and end are 9 m apart)
        private const val LOOP = "paths/durfee short.gpx"

        // Ends 543 m from where it started
        private const val OUT_AND_BACK = "paths/durfee loop.gpx"
    }
}
