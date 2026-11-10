package com.kylecorry.trail_sense.test_utils

import android.graphics.Bitmap.CompressFormat
import android.util.Base64
import android.util.Log
import androidx.test.core.app.takeScreenshot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.ByteArrayOutputStream


class ScreenshotFailureRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description?) {
        super.failed(e, description)
        val screenshot = takeScreenshot()
        val stream = ByteArrayOutputStream()
        screenshot.compress(CompressFormat.WEBP, 25, stream)
        val bitmapBytes = stream.toByteArray()
        val encodedImage = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
        val chunked = encodedImage.chunked(1000)
        chunked.forEach {
            Log.e("Screenshot", it)
        }
    }

    override fun starting(description: Description?) {
        super.starting(description)
        currentDump = dumpFile(description)
        currentDump?.delete()
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        dumpFile(description)?.delete()
    }

    private fun dumpFile(description: Description?): File? {
        return try {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val dir = File(instrumentation.targetContext.externalCacheDir, "ui-dumps")
            dir.mkdirs()
            File(dir, "${description?.className?.substringAfterLast('.')}_${description?.methodName}.xml")
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private var currentDump: File? = null

        /**
         * Saves the UI hierarchy (Compose test tags appear as resource ids) while the tool is
         * still open. Called from the test tear down, before the activity is closed; the dump is
         * deleted again if the test passes. Inspect with `adb pull <externalCacheDir>/ui-dumps`.
         */
        fun dumpHierarchy() {
            val file = currentDump ?: return
            try {
                file.outputStream().use {
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                        .dumpWindowHierarchy(it)
                }
            } catch (e: Exception) {
                Log.e("Screenshot", "Unable to dump the UI hierarchy", e)
            }
        }
    }
}
