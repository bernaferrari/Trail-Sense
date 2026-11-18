package com.kylecorry.trail_sense.desktop

import com.kylecorry.andromeda.files.appFiles
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kylecorry.trail_sense.main.SharedApp
import com.kylecorry.trail_sense.main.persistence.jvmAppDatabase
import com.kylecorry.trail_sense.main.registerSharedApp
import com.kylecorry.trail_sense.settings.backup.FilePortableBackup
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.images.ComposeAssetImages
import com.kylecorry.trail_sense.shared.images.ComposeBundledImages
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.PortablePlatformFormats
import com.kylecorry.trail_sense.shared.text.portableSystemString
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import java.util.Locale

fun main() {
    FileKit.init(appId = "com.kylecorry.trail_sense")
    val strings = ComposeAppStrings(::portableSystemString)
    val database = jvmAppDatabase(FileKit.filesDir.path + "/trail-sense.db")
    val files = appFiles()
    registerSharedApp(
        database = database,
        deviceProfile = DesktopDeviceProfile(),
        strings = strings,
        bundledImages = ComposeBundledImages(),
        images = ComposeAssetImages(),
        formats = PortablePlatformFormats(),
        fileStorage = files,
        portableBackup = FilePortableBackup(database, files)
    )

    application {
        Window(onCloseRequest = ::exitApplication, title = "Trail Sense") {
            SharedApp(strings)
        }
    }
    database.close()
}

private class DesktopDeviceProfile : DeviceProfile {
    override val hasCompass = false
    override val hasGyroscope = false
    override val hasBarometer = false
    override val hasHygrometer = false
    override val hasDynamicColors = false
    override val isMetricPreferred get() = Locale.getDefault().country !in setOf("US", "LR", "MM")
    override val uses24HourClock get() = isMetricPreferred
}
