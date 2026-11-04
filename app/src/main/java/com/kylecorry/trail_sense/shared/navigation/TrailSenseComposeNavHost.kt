package com.kylecorry.trail_sense.shared.navigation

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.views.compose.LocalTrailSenseBack
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationHost

@Composable
fun TrailSenseComposeNavHost(
    navigator: TrailSenseComposeNavigator,
    modifier: Modifier = Modifier,
    isTabRoot: (Int) -> Boolean = { false }
) {
    if (!navigator.isReady) {
        return
    }
    val backStack by navigator.backStack.collectAsState()
    val current = backStack.lastOrNull() ?: return
    val depth = backStack.size
    val activityForBack = LocalContext.current.findMainActivity()
    // Screens opened from another one get a back arrow (TrailSensePage reads it); a tab's first screen doesn't.
    val back: (() -> Unit)? = if (!isTabRoot(current.destination.id) && depth > 1 && activityForBack != null) {
        { activityForBack.onBackPressedDispatcher.onBackPressed() }
    } else {
        null
    }
    var lastDepth by remember { mutableIntStateOf(depth) }
    val forward = depth >= lastDepth
    SideEffect { lastDepth = depth }
    // The family's shared-axis motion: forward slides in from the right over a fade, back slides from the left;
    // switching tabs (same depth, a new root) fades through.
    AnimatedContent(
        targetState = current,
        contentKey = { it.id },
        transitionSpec = {
            val sameDepth = isTabRoot(targetState.destination.id) && isTabRoot(initialState.destination.id)
            if (sameDepth) {
                fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(90))
            } else {
                val direction = if (forward) 1 else -1
                (slideInHorizontally(tween(320, easing = EaseOutCubic)) { it / 6 * direction } +
                        fadeIn(tween(220, delayMillis = 40))) togetherWith
                        (slideOutHorizontally(tween(320, easing = EaseOutCubic)) { -it / 10 * direction } +
                                fadeOut(tween(120)))
            }
        },
        label = "destinations",
        modifier = modifier
    ) { entry ->
    CompositionLocalProvider(LocalTrailSenseBack provides back) {
    key(entry.id) {
        val context = LocalContext.current
        val activity = context.findMainActivity()
        if (activity != null) {
            val rootView = LocalView.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val permissionAction = remember { mutableStateOf<(() -> Unit)?>(null) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val action = permissionAction.value
                permissionAction.value = null
                action?.invoke()
            }
            val arguments = entry.arguments ?: Bundle()
            val host = remember(entry.id, activity, rootView, lifecycleOwner, arguments) {
                TrailSenseComposeDestinationHost(
                    context = context,
                    activity = activity,
                    rootView = rootView,
                    arguments = arguments,
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

            host.Content(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    }
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}
