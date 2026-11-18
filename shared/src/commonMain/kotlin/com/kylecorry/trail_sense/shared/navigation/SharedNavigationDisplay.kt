package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.select_a_chapter
import com.kylecorry.trail_sense.shared.generated.resources.select_a_note
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import org.jetbrains.compose.resources.StringResource

/** Displays the shared back stack; each platform supplies its destination content and entry owners. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SharedNavigationDisplay(
    navigator: TrailSenseNavigator,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<Route>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    content: @Composable (Route) -> Unit
) {
    if (navigator.backStack.isEmpty()) return
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.popBackStack() },
        entryDecorators = entryDecorators,
        sceneStrategies = listOf(rememberListDetailSceneStrategy<Route>()),
        transitionSpec = { if (navigator.lastChangeWasTabSwitch) fadeThrough() else slide(1) },
        popTransitionSpec = { slide(-1) },
        predictivePopTransitionSpec = { slide(-1) },
        entryProvider = { route -> NavEntry(route, metadata = route.paneMetadata()) { content(it) } }
    )
}

private fun fadeThrough(): ContentTransform =
    fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(90))

private fun slide(direction: Int): ContentTransform =
    (slideInHorizontally(tween(320, easing = EaseOutCubic)) { it / 6 * direction } +
        fadeIn(tween(220, delayMillis = 40))) togetherWith
        (slideOutHorizontally(tween(320, easing = EaseOutCubic)) { -it / 10 * direction } +
            fadeOut(tween(120)))


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun Route.paneMetadata(): Map<String, Any> = when (this) {
    Route.Notes -> ListDetailSceneStrategy.listPane(Route.Notes) {
        PanePlaceholder(Res.string.select_a_note)
    }
    is Route.NoteEditor -> ListDetailSceneStrategy.detailPane(Route.Notes)
    is Route.SurvivalGuideList -> ListDetailSceneStrategy.listPane(Route.SurvivalGuideList()) {
        PanePlaceholder(Res.string.select_a_chapter)
    }
    is Route.SurvivalGuideReader -> ListDetailSceneStrategy.detailPane(Route.SurvivalGuideList())
    else -> emptyMap()
}

@Composable
private fun PanePlaceholder(message: StringResource) {
    Box(Modifier.fillMaxSize().padding(TrailSenseSpacing.lg), contentAlignment = Alignment.Center) {
        Text(appString(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
