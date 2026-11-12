package com.kylecorry.trail_sense.tools.clinometer

import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.views.view
import androidx.test.uiautomator.By
import com.kylecorry.andromeda.camera.AndroidCamera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test
import kotlin.text.Regex

class ToolClinometerTest : ToolTestBase(ToolIds.CLINOMETER) {

    @Test
    fun verifyBasicFunctionality() {
        // Starts with the camera enabled
        if (AndroidCamera.hasBackCamera(context)) {
            isTrue(10000) {
                isCameraInUse(isBackFacing = true)
            }
        }

        canEstimateAvalancheRisk()
        canMeasureAngleAndSlope()
        canLock()
        canSwitchToDialMode()
        switchToCurrentMode()
        doesNotEstimateHeightOrDistanceByDefault()
        canEstimateDistance()
        canEstimateHeight()
    }

    private fun canLock() {
        // Lock
        tapScreenCenter()

        // TODO: Verify it is locked

        // Unlock
        tapScreenCenter()
    }

    private fun canSwitchToDialMode() {
        click(string(R.string.clinometer_mode_phone), exact = true)

        if (AndroidCamera.hasBackCamera(context)) {
            isTrue {
                !isCameraInUse(isBackFacing = true)
            }
        }
    }

    private fun switchToCurrentMode(){
        optional {
            hasText("Hold your phone vertically", waitForTime = 1000)
            click(string(R.string.camera), exact = true)
        }
    }

    private fun canEstimateAvalancheRisk() {
        hasText(string(R.string.avalanche_risk), exact = true)
        hasText(Regex("Low|Moderate|High"), exact = true)
    }

    private fun canMeasureAngleAndSlope() {
        hasText(Regex("-?\\d+°"), exact = true)
        hasText(Regex("-?\\d+% slope"))
    }

    private fun doesNotEstimateHeightOrDistanceByDefault() {
        // Until something is measured the row offers to measure
        hasText(string(R.string.height), exact = true)
        hasText(string(R.string.measure), exact = true)
    }

    private fun canEstimateDistance() {
        click(string(R.string.measure), exact = true)

        click(string(R.string.distance))
        // Picking from the list opens the distance dialog, there is no confirmation

        input(distanceField(0), "5")
        input(distanceField(1), "5")

        clickOk()

        // Instructions dialog
        not { distanceField(0)() }
        clickOk()

        hasText(Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"), exact = true)
        hasText(string(R.string.distance), exact = true)
    }

    private fun canEstimateHeight() {
        // The row is named for what it last measured
        click(string(R.string.distance), exact = true)

        click(string(R.string.height))
        // Picking from the list opens the distance dialog, there is no confirmation

        input(distanceField(0), "5")

        clickOk()

        // Instructions dialog
        not { distanceField(0)() }
        clickOk()

        hasText(Regex("(-|\\d+(\\.\\d+)?) (mi|ft)"), exact = true)
        hasText(string(R.string.height), exact = true)
    }

    /** The distance dialog's text fields, top to bottom (feet then inches when both are shown). */
    private fun distanceField(index: Int) = { view(By.clazz("android.widget.EditText"), index) }

    /** The reading locks while the screen below the bar is held, and a tap is a short hold. */
    private fun tapScreenCenter() {
        TestUtils.device.click(TestUtils.device.displayWidth / 2, TestUtils.device.displayHeight / 2)
    }
}
