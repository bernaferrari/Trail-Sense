package com.kylecorry.trail_sense.tools.map

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOkIfShown
import com.kylecorry.trail_sense.test_utils.views.desc
import android.graphics.Point
import androidx.annotation.StringRes
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import java.util.regex.Pattern
import kotlin.math.roundToInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ToolMapTest : ToolTestBase(Tools.MAP, location) {

    // The map tool only draws when there is a visible offline map
    override fun seedData() {
        TestData.addOfflineMap(MAP_NAME)
    }

    @Test
    fun verifyBasicFunctionality() {
        // The map no longer opens with a disclaimer, but accept one if it does
        clickOkIfShown()

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
        verifySensorStatusBadges()
    }

    @Test
    fun verifyTimeSlider() {
        // The map no longer opens with a disclaimer, but accept one if it does
        clickOkIfShown()

        // Open the layers sheet
        click(mapControl(R.string.map_options))
        click("Layers")

        // Enable Night layer (time dependent)
        scrollUntil { click("Additional layers") }
        click("Night")
        clickOk()

        // Close layers sheet
        back()

        // Open time sheet
        click(mapControl(R.string.time))

        // Verify elements exist
        hasText(Regex("\\d{1,2}:\\d{2} [AP]M"))

        // Reset/Close
        click(mapControl(R.string.time))
    }

    @Test
    fun verifyMapLayers() {
        // The map no longer opens with a disclaimer, but accept one if it does
        clickOkIfShown()

        // Open the layers sheet
        click(mapControl(R.string.map_options))
        click("Layers")

        // Verify the default layers are present
        scrollUntil { hasText("Location") }
        scrollUntil { hasText(string(R.string.beacons)) }
        scrollUntil { hasText("Paths") }
        scrollUntil { hasText("Tides") }
        scrollUntil { hasText("Navigation") }
        scrollUntil { hasText("Contours") }
        scrollUntil { hasText("Photo maps") }
        scrollUntil { hasText("Hillshade") }
        scrollUntil { hasText("Elevation") }
        scrollUntil { hasText("Trail maps") }
        scrollUntil { hasText("Basemap") }

        // Add an additional layer (Slope layer)
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Slope")
        clickOk()

        // Verify the new layer appears
        hasText("Slope")

        // Expand the layer
        click("Slope")

        // Move the layer down
        click(desc(string(R.string.move_layer_down)))

        // Move the layer up
        click(desc(string(R.string.move_layer_up)))

        // Toggle visible (and then re-enable)
        click("Visible")
        click("Visible")
        scrollUntil { hasText("Opacity") }

        // Copy layer to other maps
        scrollUntil { hasText("Copy settings to other maps") }
        click("Copy settings to other maps")
        // Verify Navigation is checked, uncheck Photo maps
        isChecked("Navigation")
        isChecked("Photo maps")
        click("Photo maps")
        isNotChecked("Photo maps")
        clickOk()

        // Verify the "High resolution" toggle is disabled, then turn it on
        scrollUntil { hasText("High resolution") }
        click("High resolution")

        // Remove layer
        scrollUntil { hasText("Remove layer") }
        click("Remove layer")
        clickOk()

        // Re-add all layers
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Aspect")
        click("Cell towers")
        click("Lunar eclipse")
        click("Night")
        scrollUntil { click("Ruggedness") }
        scrollUntil { click("Sightings") }
        scrollUntil { hasText("Slope") }
        scrollUntil { click("Solar eclipse") }
        clickOk()

        // Close sheet
        back()
    }

    private fun verifySensorStatusBadges() {
        click(sensorStatusBadges)
        hasText(string(R.string.accuracy_info_title))
        clickOk()
    }

    private fun canLock() {
        click(mapControl(R.string.map_lock_mode))
        click(mapControl(R.string.map_lock_mode))
        click(mapControl(R.string.map_lock_mode))
    }

    private fun canZoom() {
        click(mapControl(R.string.zoom_in))
        click(mapControl(R.string.zoom_out))
    }

    private fun canLongPressMap() {
        longPressCenter()
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText(string(R.string.beacon), exact = true)
        hasText("Navigate")
        hasText("Distance")

        click(string(R.string.beacon), exact = true)
        hasText(string(R.string.create_beacon))
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
        longPressCenter()

        click("Navigate")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        click(desc(string(R.string.stop)))

        longPressCenter()
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(closeDistance)
    }

    private fun verifyMapMenuOptions() {
        click(mapControl(R.string.map_options))
        click("Measure")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(closeDistance)

        click(mapControl(R.string.map_options))
        click("Create path")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(closeDistance)

        click(mapControl(R.string.map_options))
        click("Layers")
        back()
    }

    // The map is drawn on a canvas, so these tests find where something should be on the screen using
    // the map's projection and then interact with that point.

    private fun openWithCamp() {
        assumeFalse(isStagingBuild)
        relaunchTool {
            TestData.addOfflineMap(MAP_NAME)
            TestData.addBeacon("Camp", campLocation, elevation = 100f)
        }
    }

    @Test
    fun mapStartsCenteredOnTheCurrentLocation() {
        assumeFalse(isStagingBuild)
        clickOkIfShown()

        longPressCenter()

        val shown = parseCoordinate(waitFor { coordinateText() })
        // The screen centre lands about 60 m from the location (pixel and inset rounding), so allow ~110 m
        assertEquals(location.latitude, shown.latitude, 0.001)
        assertEquals(location.longitude, shown.longitude, 0.001)
    }

    @Test
    fun tappingABeaconShowsItAndCanNavigateToIt() {
        openWithCamp()
        clickOkIfShown()

        tapAt(campLocation) {
            hasText("Camp", exact = true, waitForTime = 1000)
        }
        hasText("42.031500°,  -71.970000°")

        click("Navigate", exact = true)

        hasText("Camp", exact = true)
        // 0.0015 degrees of latitude is 167 m
        hasDataPoint(Regex("54[78] ft"), string(R.string.navigation_remaining))
    }

    @Test
    fun longPressingReportsTheLocationDistanceAndDirection() {
        assumeFalse(isStagingBuild)
        clickOkIfShown()
        // About 223 m north and 82 m east of the current location
        val target = Coordinate(42.032, -71.969)

        longPressAt(target)

        // Shown at the pixel that was pressed, which is within a pixel (2 m) of the target
        val shown = waitFor {
            parseCoordinate(coordinateText())
        }
        assertEquals(target.latitude, shown.latitude, 0.0001)
        assertEquals(target.longitude, shown.longitude, 0.0001)

        val expectedFeet = distanceMeters(location, shown) * 3.28084
        val expectedBearing = bearingDegrees(location, shown)
        // The long press sheet should say how far and which way the point is from you
        val distance = Regex("^([\\d,]+) ft$")
            .find(textMatching(Regex("^[\\d,]+ ft$")))!!.groupValues[1].replace(",", "").toInt()
        val direction = Regex("^(\\d+)°")
            .find(textMatching(Regex("^\\d+° [NSEW]+$")))!!.groupValues[1].toInt()
        assertEquals(expectedFeet, distance.toDouble(), expectedFeet * 0.02)
        assertEquals(expectedBearing, direction.toDouble(), 1.5)
        hasText(Regex("Elevation: \\d+ ft"))
    }

    @Test
    fun canEditPathOnMap() {
        startDrawingPath()

        tapAt(pathStart)
        tapAt(pathEast)
        tapAt(pathSouth)
        assertMeasuredPath(listOf(pathStart, pathEast, pathSouth))

        // Tapping an existing point adds a point at the same location, which closes the loop
        tapAt(pathStart)
        assertMeasuredPath(listOf(pathStart, pathEast, pathSouth, pathStart))

        click(desc(string(R.string.undo)))
        assertMeasuredPath(listOf(pathStart, pathEast, pathSouth))

        click(desc(string(R.string.redo)))
        assertMeasuredPath(listOf(pathStart, pathEast, pathSouth, pathStart))

        // Dragging a point moves it, and undoing reverts the whole drag
        val moved = Coordinate(pathEast.latitude + 0.0005, pathEast.longitude)
        drag(pathEast, moved)
        assertMeasuredPath(listOf(pathStart, moved, pathSouth, pathStart))

        click(desc(string(R.string.undo)))
        assertMeasuredPath(listOf(pathStart, pathEast, pathSouth, pathStart))

        // The next point is added after the selected point instead of at the end
        longPressAt(pathEast)
        val added = Coordinate(pathEast.latitude + 0.0005, pathEast.longitude + 0.0005)
        tapAt(added)
        assertMeasuredPath(listOf(pathStart, pathEast, added, pathSouth, pathStart))
    }

    private fun startDrawingPath() {
        assumeFalse(isStagingBuild)
        clickOkIfShown()
        // Long presses don't report locations while measuring, so find the projection first
        projection()
        click(mapControl(R.string.map_options))
        click("Create path")
        isVisible(desc(string(R.string.undo)))
    }

    private fun tapAt(coordinate: Coordinate) {
        val point = getScreenPoint(coordinate)
        TestUtils.device.click(point.x, point.y)
        // Taps close together are a double tap, which zooms
        delay(DOUBLE_TAP_PAUSE_MS)
    }

    private fun drag(from: Coordinate, to: Coordinate) {
        val start = getScreenPoint(from)
        val end = getScreenPoint(to)
        TestUtils.device.swipe(start.x, start.y, end.x, end.y, 20)
    }

    private fun assertMeasuredPath(points: List<Coordinate>) {
        val expected = points.zipWithNext { a, b -> distanceMeters(a, b) }.sum()
        waitFor {
            val measured = measuredMeters()
            // The distance is shown with two decimals in miles, so allow for the rounding
            assertEquals(expected, measured, expected * 0.03 + 17)
        }
    }

    private fun measuredMeters(): Double {
        val text = textMatching(Regex("^[\\d,.]+ (ft|mi)$"))
        val (value, unit) = Regex("([\\d,.]+) (ft|mi)").find(text)!!.destructured
        val amount = value.replace(",", "").toDouble()
        return amount * if (unit == "mi") 1609.344 else 0.3048
    }

    private fun textMatching(
        regex: Regex,
        timeout: Long = AutomationLibrary.DEFAULT_WAIT_FOR_TIMEOUT
    ): String {
        return waitFor(timeout) {
            TestUtils.device.findObjects(By.text(regex.toPattern())).first().text
        }
    }

    private fun coordinateText(): String {
        return textMatching(COORDINATE)
    }

    private fun getScreenPoint(coordinate: Coordinate): Point {
        return projection().toPoint(coordinate)
    }

    /**
     * The map is a Compose canvas with no view to ask for its projection, so the test measures it: long pressing
     * reports the location under the finger, and three presses give the affine map from screen pixels to
     * coordinates. The map stays put while the test taps, so it is measured once.
     */
    private fun projection(): ScreenProjection {
        projection?.let { return it }
        val device = TestUtils.device
        val x = device.displayWidth / 2
        val y = device.displayHeight / 2
        val step = device.displayWidth / 6
        val origin = locationAt(x, y)
        val right = locationAt(x + step, y)
        val down = locationAt(x, y + step)
        return ScreenProjection(
            Point(x, y),
            origin,
            (right.latitude - origin.latitude) / step,
            (down.latitude - origin.latitude) / step,
            (right.longitude - origin.longitude) / step,
            (down.longitude - origin.longitude) / step
        ).also { projection = it }
    }

    private fun locationAt(x: Int, y: Int): Coordinate {
        // Holding a swipe in place for a second is a long press
        TestUtils.device.swipe(x, y, x, y, 200)
        val location = parseCoordinate(waitFor { coordinateText() })
        back()
        waitFor { TestUtils.not { coordinateText() } }
        return location
    }

    private fun longPressCenter() {
        val device = TestUtils.device
        device.swipe(device.displayWidth / 2, device.displayHeight / 2, device.displayWidth / 2, device.displayHeight / 2, 200)
    }

    private var projection: ScreenProjection? = null

    /** latitude = origin + dLatDx * dx + dLatDy * dy, and the same for longitude. */
    private class ScreenProjection(
        private val originPixel: Point,
        private val origin: Coordinate,
        private val dLatDx: Double,
        private val dLatDy: Double,
        private val dLonDx: Double,
        private val dLonDy: Double
    ) {
        fun toPoint(coordinate: Coordinate): Point {
            val dLat = coordinate.latitude - origin.latitude
            val dLon = coordinate.longitude - origin.longitude
            val determinant = dLatDx * dLonDy - dLatDy * dLonDx
            val dx = (dLat * dLonDy - dLatDy * dLon) / determinant
            val dy = (dLatDx * dLon - dLat * dLonDx) / determinant
            return Point(originPixel.x + dx.roundToInt(), originPixel.y + dy.roundToInt())
        }
    }

    private val closeDistance = desc(string(android.R.string.cancel))

    private fun mapControl(@StringRes description: Int) = desc(string(description))

    private val sensorStatusBadges = {
        view(
            By.desc(
                Pattern.compile(
                    "(?s).*(${Pattern.quote(string(R.string.gps))}|${Pattern.quote(string(R.string.pref_compass_sensor_title))}).*"
                )
            )
        )
    }

    /**
     * Taps the coordinate until [verify] passes, since the layers load in the background.
     */
    private fun tapAt(coordinate: Coordinate, verify: () -> Unit) {
        waitFor(10000) {
            val point = getScreenPoint(coordinate)
            TestUtils.device.click(point.x, point.y)
            verify()
        }
    }

    private fun longPressAt(coordinate: Coordinate) {
        val point = getScreenPoint(coordinate)
        // Holding a swipe in place for a second is a long press
        TestUtils.device.swipe(point.x, point.y, point.x, point.y, 200)
    }

    private fun parseCoordinate(text: String): Coordinate {
        val (latitude, longitude) = Regex("(-?\\d+\\.\\d+)°,\\s+(-?\\d+\\.\\d+)°")
            .find(text)!!.destructured
        return Coordinate(latitude.toDouble(), longitude.toDouble())
    }

    private fun distanceMeters(from: Coordinate, to: Coordinate): Double {
        val radius = 6371008.8
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * radius * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun bearingDegrees(from: Coordinate, to: Coordinate): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    companion object {
        private const val DOUBLE_TAP_PAUSE_MS = 500L
        private const val MAP_NAME = "Test Map"
        private val COORDINATE = Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°")
        private val location = Coordinate(42.03, -71.97)
        // About 167 m north of the current location
        private val campLocation = Coordinate(42.0315, -71.97)

        // Points for drawing a path, far enough apart to tap separately
        private val pathStart = Coordinate(42.0325, -71.9715)
        private val pathEast = Coordinate(42.0325, -71.9685)
        private val pathSouth = Coordinate(42.0305, -71.9685)
    }
}
