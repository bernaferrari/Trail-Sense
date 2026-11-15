package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/** Displays the shared back stack; each platform supplies its destination content and entry owners. */
@Composable
fun SharedNavigationDisplay(
    navigator: TrailSenseNavigator,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<Route>> = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    content: @Composable (Route) -> Unit
) {
    if (navigator.backStack.isEmpty()) return
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.popBackStack() },
        entryDecorators = entryDecorators,
        transitionSpec = { if (navigator.lastChangeWasTabSwitch) fadeThrough() else slide(1) },
        popTransitionSpec = { slide(-1) },
        predictivePopTransitionSpec = { slide(-1) },
        entryProvider = { route -> NavEntry(route) { content(it) } }
    )
}

private fun fadeThrough(): ContentTransform =
    fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(90))

private fun slide(direction: Int): ContentTransform =
    (slideInHorizontally(tween(320, easing = EaseOutCubic)) { it / 6 * direction } +
        fadeIn(tween(220, delayMillis = 40))) togetherWith
        (slideOutHorizontally(tween(320, easing = EaseOutCubic)) { -it / 10 * direction } +
            fadeOut(tween(120)))
