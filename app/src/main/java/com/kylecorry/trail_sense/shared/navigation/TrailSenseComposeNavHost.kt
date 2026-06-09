package com.kylecorry.trail_sense.shared.navigation

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier
) {
    if (!navigator.isReady) {
        return
    }
    val backStack by navigator.backStack.collectAsState()
    val entry = backStack.lastOrNull() ?: return
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
                modifier = modifier
            )
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
