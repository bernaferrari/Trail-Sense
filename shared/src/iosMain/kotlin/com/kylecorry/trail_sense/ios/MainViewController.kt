package com.kylecorry.trail_sense.ios

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.kylecorry.trail_sense.main.SharedApp
import com.kylecorry.trail_sense.main.persistence.iosAppDatabase
import com.kylecorry.trail_sense.main.registerSharedApp
import com.kylecorry.trail_sense.shared.images.IosAppImages
import com.kylecorry.trail_sense.shared.images.ComposeBundledImages
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.kylecorry.trail_sense.shared.text.appString
import com.mohamedrejeb.calf.sf.symbols.SFSymbol
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.AdaptiveScaffold
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreMotion.CMMotionManager
import platform.UIKit.UIViewController

@OptIn(ExperimentalCalfUiApi::class, ExperimentalForeignApi::class)
fun MainViewController(databasePath: String): UIViewController {
    val strings = ComposeAppStrings()
    registerSharedApp(
        database = iosAppDatabase(databasePath),
        deviceProfile = IosDeviceProfile(),
        strings = strings,
        bundledImages = ComposeBundledImages(),
        images = IosAppImages(),
        formats = IosPlatformFormats(),
        hasMotion = CMMotionManager().deviceMotionAvailable,
        hasCamera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null
    )

    return ComposeUIViewController {
        SharedApp(strings) { tabs, selectedIndex, select, content ->
            AdaptiveScaffold(bottomBar = {
                if (selectedIndex >= 0) {
                    AdaptiveNavigationBar(
                        iosItems = tabs.mapIndexed { index, tab ->
                            val (icon, selectedIcon) = tabSymbols[index]
                            UIKitUITabBarItem(
                                title = appString(tab.label),
                                image = UIKitImage.SystemName(icon),
                                selectedImage = UIKitImage.SystemName(selectedIcon)
                            )
                        },
                        iosSelectedIndex = selectedIndex,
                        iosOnItemSelected = select,
                        iosConfiguration = UIKitTabBarConfiguration(
                            selectedItemColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }, content = content)
        }
    }
}

private val tabSymbols = listOf(
    SFSymbol.location to SFSymbol.locationFill,
    SFSymbol.wrenchAndScrewdriver to SFSymbol.wrenchAndScrewdriverFill,
    SFSymbol.cloudSun to SFSymbol.cloudSunFill,
    SFSymbol.gearshape to SFSymbol.gearshape
)
