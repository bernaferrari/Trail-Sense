package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.camera.AndroidCamera
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolAugmentedRealityTest : ToolTestBase(ToolIds.AUGMENTED_REALITY) {

    @Test
    fun verifyBasicFunctionality() {
        if (!getAppService<ToolCatalog>().isToolAvailable(ToolIds.AUGMENTED_REALITY)) {
            return
        }

        // Click OK on the prompts
        clickOk()

        if (!Sensors.hasGyroscope(context)) {
            clickOk()
        }

        if (AndroidCamera.hasBackCamera(context)) {
            isTrue(10000) {
                isCameraInUse(isBackFacing = true)
            }
        }

        canTurnOffCamera()
        canCalibrate()
        canToggleLayers()
    }

    private fun canTurnOffCamera() {
        click(desc(string(R.string.camera)))

        if (AndroidCamera.hasBackCamera(context)) {
            isTrue {
                !isCameraInUse(isBackFacing = true)
            }
        }
    }

    private fun canCalibrate() {
        click(desc(string(R.string.calibrate)))
        clickOk()
        click(string(R.string.calibrate), exact = true)
    }

    private fun canToggleLayers() {
        click(desc(string(R.string.map_layers)))
        // Verify the layers sheet is visible
        hasText(string(R.string.layers))
        hasText(string(R.string.beacons))
        hasText(string(R.string.paths))

        // Turn off the beacons layer
        click(string(R.string.beacons), exact = true)

        // Close the layers sheet
        back()
        not { hasText(string(R.string.grid), exact = true) }
    }
}
