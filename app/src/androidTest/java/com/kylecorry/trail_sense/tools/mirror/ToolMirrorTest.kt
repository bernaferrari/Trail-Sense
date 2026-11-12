package com.kylecorry.trail_sense.tools.mirror

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.isCameraInUse
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolMirrorTest : ToolTestBase(ToolIds.MIRROR_CAMERA) {
    @Test
    fun verifyBasicFunctionality() {
        if (!getAppService<ToolCatalog>().isToolAvailable(ToolIds.MIRROR_CAMERA)) {
            return
        }

        isVisible(R.id.camera)

        isTrue(10000) {
            isCameraInUse(isBackFacing = false)
        }
    }
}
