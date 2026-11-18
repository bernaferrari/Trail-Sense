package com.kylecorry.trail_sense.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop

@Composable
internal fun BindBrowserHistory(navigator: TrailSenseNavigator) {
    DisposableEffect(navigator) {
        navigator.restoreState(readStack())
        if (readStack() == null) writeStack(navigator.saveState(), null, replace = true)

        var handlingBrowserEvent = false
        var savedStack = navigator.saveState()
        val routeListener = TrailSenseNavigator.OnRouteChangedListener { _, _ ->
            val next = navigator.saveState()
            if (!handlingBrowserEvent && next != savedStack) {
                if (next == readPreviousStack()) {
                    window.history.back()
                } else {
                    writeStack(next, savedStack, replace = false)
                }
            }
            savedStack = next
        }
        val browserListener: (Event) -> Unit = {
            handlingBrowserEvent = true
            try {
                if (!navigator.restoreState(readStack())) navigator.setStartRoute(Route.Tools)
            } finally {
                handlingBrowserEvent = false
            }
        }
        navigator.addOnRouteChangedListener(routeListener)
        window.addEventListener("popstate", browserListener)
        onDispose {
            window.removeEventListener("popstate", browserListener)
            navigator.removeOnRouteChangedListener(routeListener)
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readStack(): String? = js("window.history.state?.trailSenseStack ?? null")

@OptIn(ExperimentalWasmJsInterop::class)
private fun readPreviousStack(): String? = js("window.history.state?.trailSensePreviousStack ?? null")

@OptIn(ExperimentalWasmJsInterop::class)
private fun writeStack(stack: String, previous: String?, replace: Boolean): Unit = js(
    """{
        const state = { trailSenseStack: stack, trailSensePreviousStack: previous };
        if (replace) window.history.replaceState(state, "");
        else window.history.pushState(state, "");
    }"""
)
