package com.kylecorry.trail_sense.tools.beacons

import com.kylecorry.trail_sense.test_utils.views.field
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInVerticalOrder
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasTextsInOrder
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assume.assumeFalse
import org.junit.Test

class ToolBeaconsTest : ToolTestBase(Tools.BEACONS, Coordinate(42.03, -71.97)) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(id("beacon_title"), string(R.string.places_title))

        // No beacons by default
        hasText(id("beacon_empty_text"), string(R.string.no_places))

        createBeacon()
        openBeacon()
        shareBeacon()
        editBeacon()
        navigate()
        createGroup()
        moveBeacon()
        search()
        deleteBeacon()
        toggleVisibility()
        renameGroup()
        deleteGroup()
    }

    private fun createBeacon() {
        // An empty list offers its own "Save a place" button instead of the create menu
        any(
            { click(id("save_place_btn"), waitForTime = 0) },
            {
                click(id("create_btn"))
                click(string(R.string.beacon), exact = true)
            }
        )

        hasText(string(R.string.create_beacon))

        input(id("beacon_name"), "Test beacon")
        input(id("beacon_location"), "42, -72")
        input(id("beacon_elevation"), "1000")

        isChecked(id("create_at_distance"), false)

        scrollToEnd(id("create_beacon_scroll"))

        hasText(id("beacon_group_picker"), string(R.string.no_group))
        hasText(id("beacon_color_picker"), string(R.string.places_shape_and_color))
        hasText(id("beacon_icon_picker"), string(R.string.icon))

        input(id("comment"), "Test notes")

        click(id("create_beacon_save_btn"))

        hasText(id("beacon_title"), string(R.string.places_title))
        hasText("Test beacon")
    }

    private fun openBeacon() {
        click("Test beacon")
        hasText("Test beacon")
        hasText("42.000000°,  -72.000000°")

        hasText(id("beacon_altitude"), "1000 ft")
        hasText(id("beacon_altitude"), string(R.string.elevation))
        hasText(
            Regex("\\d+\\.?\\d* (mi|ft)"),
            waitForTime = AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
        )
        // The distance is the page's unlabelled headline
        hasText(id("beacon_distance"), Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText(Regex("\\d+ °F / \\d+ °F"))
        hasText(id("beacon_temperature"), string(R.string.temperature_high_low))
        hasText(Regex("\\d+:\\d+ (AM|PM)"))
        hasText(id("beacon_sunrise"), string(R.string.sunrise))
        hasText(Regex("\\d+:\\d+ (AM|PM)"))
        hasText(id("beacon_sunset"), string(R.string.sunset))
        hasText(Regex("(High|Low|Half)"))
        hasText(id("beacon_tide"), string(R.string.tide))
        hasText(id("comment_text"), "Test notes")
    }

    private fun shareBeacon() {
        click(id("beacon_menu_btn"))
        click(string(R.string.share_ellipsis))

        hasText(string(android.R.string.copy))
        hasText(string(R.string.qr_code))
        hasText(string(R.string.maps))
        hasText(string(R.string.share_action_send))

        back(false)
    }

    private fun editBeacon() {
        click(id("edit_btn"))
        // Editing says so in the title
        hasText(string(R.string.edit_beacon))
        hasText(id("beacon_name"), "Test beacon")
        hasText(id("beacon_location"), "42.000000°,  -72.000000°")
        hasText(id("beacon_elevation"), "1000")
        isChecked(id("create_at_distance"), false)

        scrollToEnd(id("create_beacon_scroll"))
        hasText(id("comment"), "Test notes")
        hasText(id("beacon_group_picker"), string(R.string.no_group))
        hasText(id("beacon_color_picker"), string(R.string.places_shape_and_color))
        hasText(id("beacon_icon_picker"), string(R.string.icon))

        input(id("comment"), "Test notes 2")

        click(id("create_beacon_save_btn"))

        hasText("Test beacon")
        hasText(id("comment_text"), "Test notes 2")
    }

    private fun navigate() {
        click(string(R.string.navigate))

        clickOkIfShown()

        isVisible(R.id.navigation_title)
        hasText("Test beacon")
        back()
        back()
    }

    private fun deleteBeacon() {
        click("Test group")
        click(id("beacon_item_menu"))
        click("Delete")
        clickOk()
        hasText("Test beacon 2")
        not { hasText("Test beacon", exact = true) }
        back(false)
    }

    private fun toggleVisibility() {
        click("Test group")
        click(id("beacon_visibility_toggle"))
        click(id("beacon_visibility_toggle"))
        back(false)
    }

    private fun createGroup() {
        click(id("create_btn"))
        click(string(R.string.group))

        input(string(R.string.name), "Test group")
        clickOk()

        hasText("Test group")
        hasText("0 places")
        click("Test group")
        hasText(string(R.string.no_places))

        createBeaconInGroup()
    }

    private fun createBeaconInGroup() {
        click(id("create_btn"))
        click(string(R.string.beacon), exact = true)

        input(id("beacon_name"), "Test beacon 2")
        input(id("beacon_location"), "42, -72")

        scrollToEnd(id("create_beacon_scroll"))
        hasText("Test group")

        click(id("create_beacon_save_btn"))

        hasText("Test group")
        hasText("Test beacon 2")
        back(false)
        hasText("Test group")
        hasText("1 place")
        hasText("Test beacon")
    }

    private fun moveBeacon() {
        click(id("beacon_item_menu"))
        click("Move to")
        click("Test group")
        click("Move")
        hasText("2 places")
        not { hasText("Test beacon", exact = true) }

        click("Test group")
        hasText("Test beacon")
        hasText("Test beacon 2")
        back(false)
    }

    private fun deleteGroup() {
        click(id("beacon_group_menu"))
        click("Delete")
        clickOk()
        hasText(string(R.string.no_places))
        not { hasText("Test group", exact = true) }
    }

    private fun renameGroup() {
        click(id("beacon_group_menu"))
        click("Rename")
        input("Test group", "Test group 2")
        clickOk()
        hasText("Test group 2")
        not { hasText("Test group", exact = true) }
    }

    private fun search() {
        input(id("searchbox"), "2")
        hasText("Test beacon 2")
        input(id("searchbox"), "")
        hasText("Test group")
    }

    // Beacons are placed at known offsets from the current location (42.03, -71.97) so the distances
    // and directions can be checked. They were verified by an independent calculation:
    // - Camp: 0.69 mi due north
    // - Parking: 4.15 mi at 120 degrees
    // - Lake: 14.84 mi at 233 degrees
    // - Summit: 40.40 mi at 36 degrees

    private fun openWithSavedBeacons() {
        assumeFalse(isStagingBuild)
        relaunchTool { seedBeacons() }
    }

    private fun seedBeacons() {
        TestData.addBeacon(
            "Summit",
            Coordinate(42.5, -71.5),
            elevation = 304.8f,
            comment = "Great view"
        )
        TestData.addBeacon("Camp", Coordinate(42.04, -71.97), elevation = 100f)
        TestData.addBeacon("Lake", Coordinate(41.9, -72.2))
        val group = TestData.addBeaconGroup("Trailheads")
        TestData.addBeacon("Parking", Coordinate(42.0, -71.9), groupId = group)
    }

    @Test
    fun listIsSortedByDistanceFromTheCurrentLocation() {
        openWithSavedBeacons()

        hasTextsInVerticalOrder(listOf("Camp", "Trailheads", "Lake", "Summit"))
        // Each row's subtitle reads direction · distance · elevation
        hasTextsInVerticalOrder(
            listOf("N 0° · 0.69 mi · 328 ft", "1 place", "SW 233° · 14.86 mi", "NE 36° · 40.41 mi · 1000 ft")
        )
    }

    @Test
    fun groupsListTheirBeacons() {
        openWithSavedBeacons()

        click("Trailheads")

        hasText(R.id.beacon_title, "Trailheads")
        hasText("Parking")
        // 4.15 mi on a spherical earth
        hasText(Regex("4\\.1[56] mi"))
        not { hasText("Camp") }
    }

    @Test
    fun canSearchByName() {
        openWithSavedBeacons()

        input(R.id.searchbox, "lak")
        hasText("Lake")
        not { hasText("Summit") }
        not { hasText("Camp") }

        // Beacons inside of groups are found too
        input(R.id.searchbox, "park")
        hasText("Parking")
        not { hasText("Lake") }
    }

    @Test
    fun detailsShowTheSavedBeacon() {
        openWithSavedBeacons()

        click("Summit")

        hasText(R.id.beacon_title, "Summit")
        hasText(R.id.beacon_title, "42.500000°,  -71.500000°")
        // Stored in meters, displayed in feet
        hasDataPoint("1000 ft", string(R.string.elevation))
        // The distance is the page's headline now, not a data point
        hasText(id("beacon_distance"), "40.41 mi")
        hasText(R.id.comment_text, "Great view")
    }

    @Test
    fun canNavigateToABeacon() {
        openWithSavedBeacons()

        click("Summit")
        click(string(R.string.navigate))
        clickOkIfShown()

        // The navigation card: the destination and its distance
        isVisible(desc(string(R.string.stop)))
        hasText("Summit", exact = true)
        hasText(Regex("40\\.41( mi)?"))
    }

    @Test
    fun canCreateABeaconAtADistanceAndBearing() {
        openWithSavedBeacons()

        click(R.id.create_btn)
        click(string(R.string.beacon), exact = true)
        input(R.id.beacon_name, "Mile north")
        input(id("beacon_location"), "42, -72")
        click(R.id.create_at_distance)
        isChecked(R.id.create_at_distance)

        input(string(R.string.distance), "5280")
        // The manual entry is an icon button beside the compass and camera; its dialog hides tags, so use labels
        click(id("manual_bearing"))
        input(field(string(R.string.bearing)), "0")
        click(string(R.string.true_north), exact = true)
        clickOk()

        scrollToEnd(R.id.create_beacon_scroll)
        click(id("create_beacon_save_btn"))

        // One mile north of 42, -72
        click("Mile north")
        hasText(R.id.beacon_title, Regex("42\\.0144\\d\\d°,\\s+-72\\.0000\\d\\d°"))
        back()
    }

}
