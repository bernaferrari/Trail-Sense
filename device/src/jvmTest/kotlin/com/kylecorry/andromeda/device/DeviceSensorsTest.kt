package com.kylecorry.andromeda.device

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceSensorsTest {
    @Test fun desktopLoadsWithoutMobileSensorRuntime() = runBlocking {
        assertTrue(DeviceSensors().pressure().toList().isEmpty())
    }
}
