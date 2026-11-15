package com.kylecorry.trail_sense.shared.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.views.compose.LocalTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects

/**
 * Draws the [TrailSenseNavigator]'s back stack with Navigation 3. Each entry keeps its saved state (scroll position,
 * collapsed bar, typed text) while it is on the stack, so coming back from a sub-page returns to where you were;
 * popped entries drop theirs. System back and predictive back pop the stack.
 */
@Composable
fun TrailSenseNavDisplay(
    navigator: TrailSenseNavigator,
    modifier: Modifier = Modifier,
    isTabRoot: (Route) -> Boolean = { false }
) {
    val backStack = navigator.backStack
    if (backStack.isEmpty()) return
    val current = backStack.last()
    val activityForBack = LocalContext.current.findMainActivity()
    // Screens opened from another one get a back arrow (TrailSensePage reads it); a tab's first screen doesn't.
    val back: (() -> Unit)? = if (!isTabRoot(current) && backStack.size > 1 && activityForBack != null) {
        { activityForBack.onBackPressedDispatcher.onBackPressed() }
    } else {
        null
    }
    CompositionLocalProvider(LocalTrailSenseBack provides back, LocalNavigator provides navigator) {
        SharedNavigationDisplay(
            navigator = navigator,
            modifier = modifier,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            content = { TrailSenseRouteContent(it) }
        )
    }
}

/** One screen: its own lifecycle, permission launcher and destination host, then the screen itself. */
@Composable
private fun TrailSenseRouteContent(route: Route) {
    val context = LocalContext.current
    val activity = context.findMainActivity() ?: return
    // Each screen lives on its own lifecycle: it follows the activity's, and ends when the screen leaves, so
    // observers a screen registers (LiveData, sensors) stop with it instead of firing into a dead screen.
    val parentLifecycleOwner = LocalLifecycleOwner.current
    val lifecycleOwner = remember { DestinationLifecycleOwner(parentLifecycleOwner) }
    DisposableEffect(lifecycleOwner) {
        onDispose { lifecycleOwner.destroy() }
    }
    val permissionAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val action = permissionAction.value
        permissionAction.value = null
        action?.invoke()
    }
    val host = remember(activity, lifecycleOwner) {
        TrailSenseComposeDestinationHost(
            context = context,
            activity = activity,
            route = route,
            lifecycleOwner = lifecycleOwner
        ) { permissions, action ->
            permissionAction.value = action
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(host) {
        host.attach()
        onDispose {
            host.detach()
        }
    }

    DisposableEffect(host, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> host.onScreenResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner, LocalScreenEffects provides host) {
        host.Content(modifier = Modifier.fillMaxSize())
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}

/** A screen's lifecycle: the activity's state while the screen is shown, destroyed when the screen goes away. */
private class DestinationLifecycleOwner(private val parent: LifecycleOwner) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    private var destroyed = false
    private val follower = LifecycleEventObserver { _, _ ->
        if (!destroyed) registry.currentState = parent.lifecycle.currentState.coerceAtLeast(Lifecycle.State.CREATED)
    }

    override val lifecycle: Lifecycle
        get() = registry

    init {
        parent.lifecycle.addObserver(follower)
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        parent.lifecycle.removeObserver(follower)
        // A screen added while the activity is already destroyed never reaches CREATED, and a lifecycle can't move
        // from INITIALIZED straight to DESTROYED
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
