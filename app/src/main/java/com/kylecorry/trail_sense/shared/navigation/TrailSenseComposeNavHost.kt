package com.kylecorry.trail_sense.shared.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.views.compose.LocalTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationHost

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
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = { navigator.popBackStack() },
            // Each entry keeps its saved state and owns its ViewModels: they live while the screen is on the stack and
            // are cleared when it is popped.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            // The family's shared-axis motion: forward slides in from the right over a fade, back slides from the
            // left; switching tabs (a new root) fades through.
            transitionSpec = { if (navigator.lastChangeWasTabSwitch) fadeThrough() else slide(1) },
            popTransitionSpec = { slide(-1) },
            predictivePopTransitionSpec = { slide(-1) },
            // A route appears on the stack at most once (TrailSenseNavigator.navigate), so the route is the entry's key.
            entryProvider = { route -> NavEntry(route) { TrailSenseRouteContent(it) } }
        )
    }
}

private fun fadeThrough(): ContentTransform =
    fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(90))

private fun slide(direction: Int): ContentTransform =
    (slideInHorizontally(tween(320, easing = EaseOutCubic)) { it / 6 * direction } +
            fadeIn(tween(220, delayMillis = 40))) togetherWith
            (slideOutHorizontally(tween(320, easing = EaseOutCubic)) { -it / 10 * direction } +
                    fadeOut(tween(120)))

/** One screen: its own lifecycle, permission launcher and destination host, then the screen itself. */
@Composable
private fun TrailSenseRouteContent(route: Route) {
    val context = LocalContext.current
    val activity = context.findMainActivity() ?: return
    val rootView = LocalView.current
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
    val host = remember(activity, rootView, lifecycleOwner) {
        TrailSenseComposeDestinationHost(
            context = context,
            activity = activity,
            rootView = rootView,
            route = route,
            lifecycleOwner = lifecycleOwner
        ) { permissions, action ->
            permissionAction.value = action
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(host) {
        host.onStart()
        onDispose {
            host.onStop()
            host.cleanupReactiveEffects()
        }
    }

    DisposableEffect(host, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> host.onResume()
                Lifecycle.Event.ON_PAUSE -> host.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
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
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
