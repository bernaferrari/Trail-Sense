package com.kylecorry.andromeda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration

/** Runs while the host is resumed, cancelling immediately when it pauses. */
@Composable
fun ResumeInterval(interval: Duration, initialDelay: Duration = Duration.ZERO, action: suspend () -> Unit) {
    require(interval > Duration.ZERO && interval.isFinite() && initialDelay >= Duration.ZERO)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latest by rememberUpdatedState(action)
    LaunchedEffect(lifecycle, interval, initialDelay) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(initialDelay)
            while (currentCoroutineContext().isActive) {
                latest()
                delay(interval)
            }
        }
    }
}

@Composable
fun OnResumeAfter(delay: Duration = Duration.ZERO, action: suspend () -> Unit) {
    require(delay >= Duration.ZERO)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latest by rememberUpdatedState(action)
    LaunchedEffect(lifecycle, delay) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            kotlinx.coroutines.delay(delay)
            latest()
        }
    }
}
