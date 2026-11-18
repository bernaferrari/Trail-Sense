package com.kylecorry.trail_sense.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.kylecorry.trail_sense.main.SharedApp
import com.kylecorry.trail_sense.main.persistence.openWasmDatabase
import com.kylecorry.trail_sense.main.registerSharedApp
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import com.kylecorry.trail_sense.shared.images.ComposeAssetImages
import com.kylecorry.trail_sense.shared.images.ComposeBundledImages
import com.kylecorry.trail_sense.shared.text.WebAppStrings
import com.kylecorry.trail_sense.shared.text.PortablePlatformFormats
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.body ?: return
    ComposeViewport(root) {
        var strings by remember { mutableStateOf<WebAppStrings?>(null) }
        var failed by remember { mutableStateOf(false) }
        var attempt by remember { mutableIntStateOf(0) }

        LaunchedEffect(attempt) {
            failed = false
            root.setAttribute("aria-busy", "true")
            try {
                val loaded = WebAppStrings().apply { preload() }
                registerSharedApp(
                    database = openWasmDatabase(),
                    deviceProfile = WebDeviceProfile(),
                    strings = loaded,
                    bundledImages = ComposeBundledImages(),
                    images = ComposeAssetImages(),
                    formats = PortablePlatformFormats(),
                    mapFiles = SharedMapFiles(BrowserOfflineMapStorage(), ::renderBrowserMapPdf)
                )
                strings = loaded
                root.setAttribute("aria-busy", "false")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                println(error.stackTraceToString())
                failed = true
                root.removeAttribute("aria-busy")
            }
        }

        val loaded = strings
        if (loaded != null) {
            SharedApp(loaded, bindNavigation = { BindBrowserHistory(it) })
        } else {
            TrailSenseTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (failed) {
                        Text("Trail Sense could not start. Close other Trail Sense tabs and try again.")
                        Button(onClick = { attempt++ }) { Text("Retry") }
                    } else {
                        CircularProgressIndicator()
                        Text("Loading Trail Sense")
                    }
                }
            }
        }
    }
}
