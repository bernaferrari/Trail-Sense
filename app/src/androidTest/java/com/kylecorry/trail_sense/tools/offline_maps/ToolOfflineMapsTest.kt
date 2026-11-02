package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class ToolOfflineMapsTest : ToolTestBase(Tools.OFFLINE_MAPS) {

    @Test
    fun verifyPhotoMapsBasicFunctionality() {
        // Disclaimer
        clickOk()

        hasText(id("map_list_title"), string(R.string.offline_maps))

        // No maps by default
        hasText(string(R.string.no_maps))

        canCreateMapFromCamera()
        canToggleVisibility()
        canViewMap()
        canCreateBlankMap("Blank Map")
        canCreateMapFromFile()
        canCreateGroup()
        canRenameGroup()
        canSearch()
        canRenameMap()
        canMoveMap()
        canChangeMapResolution()
        canExportMap()
        canPrintMap()
        canDeleteGroup()
        canDeleteMap()
    }

    @Test
    fun verifyMapsforgeBasicFunctionality() {
        // TODO: To run on staging builds another solution to populating maps will need to be used
        if (AutomationLibrary.packageName != null) {
            return
        }

        // Offline maps disclaimer
        clickOkIfShown()

        hasText(id("map_list_title"), string(R.string.offline_maps))
        hasText(string(R.string.no_maps))

        canOpenOfflineMapFilePicker()
        canCreateOfflineMapGroup()

        seedOfflineMap("Second Map")
        seedOfflineMap("Test Map")
        input(id("searchbox")(), "Test")
        waitFor { hasText("Test Map") }
        input(id("searchbox")(), "")
        hasText("Second Map")
        hasText(id("map_list_title"), string(R.string.offline_maps))

        canToggleOfflineMapVisibility()
        canViewOfflineMap()
        canExportOfflineMapFromList()
        canExportOpenedOfflineMap()
        canRenameOfflineMapGroup()
        canSearchOfflineMaps()
        canRenameOfflineMap()
        canEditOfflineMapAttribution()
        canMoveOfflineMap()
        canDeleteOfflineMapGroup()
        canDeleteOfflineMap()
    }

    private fun canOpenOfflineMapFilePicker() {
        click(id("add_btn"))
        click(string(R.string.map_file))

        // The file picker is opened
        not { isVisible(id("add_btn")) }

        waitFor {
            waitFor {
                back()
            }
            isVisible(id("map_list_title"))
        }
    }

    private fun canCreateOfflineMapGroup() {
        click(id("add_btn"))
        click(string(R.string.group))

        input(string(R.string.name), "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
    }

    private fun canToggleOfflineMapVisibility() {
        click(id("map_visibility_toggle_Test Map"))
        click(id("map_visibility_toggle_Test Map"))
    }

    private fun canViewOfflineMap() {
        click("Test Map")
        hasText("Test Map")
        click(id("zoom_in_btn"))
        click(id("zoom_out_btn"))
        back()
    }

    private fun canExportOfflineMapFromList() {
        input(R.id.searchbox, "Test Map")
        clickListItemMenu(string(R.string.export))
        backUntil {
            hasText("test-map.map")
        }
        backUntil {
            isVisible(R.id.map_list_title)
        }
        input(R.id.searchbox, "")
    }

    private fun canExportOpenedOfflineMap() {
        click("Test Map")
        waitFor {
            isVisible(R.id.zoom_in_btn)
        }
        click(toolbarButton(R.id.title, Side.Right))
        click(string(R.string.export))
        backUntil {
            hasText("test-map.map")
        }
        backUntil {
            isVisible(R.id.map)
        }
        back()
    }

    private fun canRenameOfflineMapGroup() {
        click(id("map_group_menu_Test Group"))
        click(string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("0 maps")
    }

    private fun canSearchOfflineMaps() {
        input(id("searchbox")(), "Second")
        hasText("Second Map")
        not { hasText("Test Map") }
        input(id("searchbox")(), "")
        hasText("Test Group 2")
        hasText("Test Map")
    }

    private fun canRenameOfflineMap() {
        input(id("searchbox")(), "Test Map")
        hasText("Test Map")
        click(id("vector_map_menu_Test Map"))
        click(string(R.string.rename))
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
    }

    private fun canEditOfflineMapAttribution() {
        input(id("searchbox")(), "Test Map 2")
        hasText("Test Map 2")
        click(id("vector_map_menu_Test Map 2"))
        click(string(R.string.attribution))
        input(string(R.string.attribution), "Test Attribution")
        clickOk()
    }

    private fun canMoveOfflineMap() {
        input(id("searchbox")(), "Test Map 2")
        hasText("Test Map 2")
        click(id("vector_map_menu_Test Map 2"))
        click(string(R.string.move_to))
        click("Test Group 2")
        click(string(R.string.move))
        input(id("searchbox")(), "")
        hasText("Second Map")
        hasText("Test Group 2")
        hasText("1 map")
    }

    private fun canDeleteOfflineMapGroup() {
        click(id("map_group_menu_Test Group 2"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
    }

    private fun canDeleteOfflineMap() {
        input(id("searchbox")(), "Second")
        hasText("Second Map")
        click(id("vector_map_menu_Second Map"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Second Map") }
        input(id("searchbox")(), "")
        hasText(string(R.string.no_maps))
    }

    private fun seedOfflineMap(name: String) = runBlocking {
        val files = getAppService<FileSubsystem>()
        files.getDirectory("offline_maps", true)
        val filename = name.lowercase().replace(" ", "-")
        val path = "offline_maps/$filename.map"
        files.get(path, true).writeBytes(ByteArray(0))

        getAppService<MapService>().add(
            VectorMap(
                0,
                name,
                VectorMapFileType.Mapsforge,
                path,
                0,
                Instant.now(),
                null,
                null,
                visible = true
            )
        )
    }


    private fun canCreateMapFromCamera(goBack: Boolean = true) {
        click(id("add_btn"))
        click("Camera")
        click(id("capture_button"))
        input(string(R.string.name), "Test Map", index = 1)
        clickOk()

        optional {
            click("Preview")
            click("Edit")
        }

        click("Next")
        clickOkIfShown()

        hasText("Test Map")
        hasText("Rotation: 0°")

        input("Location", "42, -72")
        click("Next")

        click(R.id.calibration_map, xPercent = 0.7f, yPercent = 0.3f)
        input("Location", "42.1, -72.1")
        click(string(R.string.preview))
        click("Done")

        hasText("Test Map")
        if (goBack) {
            back()
        }
    }

    private fun canCreateMapFromFile() {
        click(id("add_btn"))
        click(string(R.string.map_file))

        // The file picker is opened
        not { isVisible(id("add_btn")) }

        waitFor {
            waitFor {
                back()
            }
            isVisible(id("map_list_title"))
        }
    }

    private fun canCreateBlankMap(name: String, goBack: Boolean = true) {
        click(id("add_btn"))
        click(string(R.string.blank))

        clickOk()
        input(string(R.string.name), name, index = 1)
        clickOk()

        hasText(name)
        if (goBack) {
            back()
        }
    }

    private fun canCreateGroup() {
        click(id("add_btn"))
        click(string(R.string.group))

        input("Name", "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
        click("Test Group")
        hasText(string(R.string.no_maps))

        // Create a map in the group
        canCreateBlankMap("Blank Map")

        hasText("Blank Map")
        back()
        hasText("Test Map")
        hasText("Test Group")
        hasText("1 map")
    }

    private fun canSearch() {
        input(id("searchbox")(), "Blank")
        hasText("Blank Map")
        hasText("Blank Map", index = 1)
        not { hasText("Test Map") }
        input(id("searchbox")(), "")
        hasText("Test Group 2")
    }

    private fun canDeleteGroup() {
        click(id("map_group_menu_Test Group 2"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Test Group 2") }
    }

    private fun canRenameGroup() {
        click(id("map_group_menu_Test Group"))
        click(string(R.string.rename))
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("1 map")
    }

    private fun canToggleVisibility() {
        click(id("map_visibility_toggle_Test Map"))
        click(id("map_visibility_toggle_Test Map"))
    }

    private fun canViewMap() {
        click("Test Map")
        hasText("Test Map")

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
        back()
    }

    private fun canLock() {
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(id("map_recenter_btn"))
    }

    private fun canZoom() {
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
        click(id("map_recenter_btn"))
    }

    private fun canLongPressMap() {
        longClick(R.id.map)
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText("Beacon")
        hasText("Navigate")
        hasText("Distance")

        click("Navigate")
        hasText("Test Map", index = 1)
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        click(R.id.navigation_cancel)

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        longClick(R.id.map)
        click("Beacon")
        hasText("Create beacon")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
    }

    private fun verifyMapMenuOptions() {
        // Calibrate
        click(id("map_menu_btn"))
        click("Calibrate")
        hasText("Point 1 of 2")
        waitFor {
            // The click can land before the page settles, so repeat it until the next step shows
            try {
                hasText("Finish calibration", waitForTime = 1)
            } catch (e: Throwable) {
                click("Next", waitForTime = 1)
                throw e
            }
        }
        click("Finish calibration")
        isVisible(R.id.map)

        // User guide
        click(id("map_menu_btn"))
        click("User Guide")
        hasText("Offline Maps")
        back()
        isVisible(R.id.map)

        // Rename
        click(id("map_menu_btn"))
        click("Rename")
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")

        // Change projection
        click(id("map_menu_btn"))
        click("Change projection")
        click("Equidistant")
        clickOk()

        // Measure
        click(id("map_menu_btn"))
        click("Measure")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        // Create path
        click(id("map_menu_btn"))
        click("Create path")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        // Layers
        click(id("map_menu_btn"))
        click("Layers")
        scrollUntil { hasText("Location") }
        scrollUntil { hasText("Beacons") }
        scrollUntil { hasText("Paths") }
        scrollUntil { hasText("Tides") }
        scrollUntil { hasText("Navigation") }
        scrollUntil { hasText("Photo maps") }
        click(toolbarButton(R.id.title, Side.Right))

        // Export
        click(id("map_menu_btn"))
        click("Export")
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("test-map-2.pdf")
        }
        backUntil {
            isVisible(R.id.map)
        }

        // Print
        click(id("map_menu_btn"))
        click("Print")
        hasText("Copies")
        backUntil {
            isVisible(R.id.map)
        }

        // Trace
        click(id("map_menu_btn"))
        click("Trace")
        clickOk()
        not { isVisible(R.id.zoom_in_btn) }
        not { isVisible(R.id.zoom_out_btn) }
        // Bottom nav does nothing
        click(R.id.bottom_navigation)
        click(R.id.lock_btn)
        isVisible(R.id.zoom_in_btn)
        isVisible(R.id.zoom_out_btn)

        // Delete
        click(id("map_menu_btn"))
        click(string(R.string.delete))
        clickOk()

        isVisible(id("map_list_title"))
        not { hasText("Test Map 2") }

        // Recreate the map
        canCreateMapFromCamera(false)
    }

    private fun canRenameMap() {
        click(id("photo_map_menu_Test Map"))
        click(string(R.string.rename))
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
    }

    private fun canMoveMap() {
        click(id("photo_map_menu_Test Map 2"))
        click(string(R.string.move_to))
        click("Test Group 2")
        click("Move")
        hasText("2 maps")
        not { hasText("Test Map 2") }
        click("Test Group 2")
        hasText("Test Map 2")
        back(false)
    }

    private fun canChangeMapResolution() {
        click(id("photo_map_menu_Blank Map"))
        click(string(R.string.change_resolution))
        click("Moderate")
        clickOk()
        waitFor { hasText("Blank Map") }
    }

    private fun canExportMap() {
        click(id("photo_map_menu_Blank Map"))
        click(string(R.string.export))
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("blank-map.pdf")
        }
        backUntil {
            isVisible(id("map_list_title"))
        }
    }

    private fun canPrintMap() {
        click(id("photo_map_menu_Blank Map"))
        click(string(R.string.print))
        hasText("Copies")
        backUntil {
            isVisible(id("map_list_title"))
        }
    }

    private fun canDeleteMap() {
        click(id("photo_map_menu_Blank Map"))
        click(string(R.string.delete))
        clickOk()
        not { hasText("Blank Map") }
    }

}
