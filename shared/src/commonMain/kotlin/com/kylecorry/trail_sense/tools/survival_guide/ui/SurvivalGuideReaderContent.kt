package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import kotlinx.coroutines.withContext

@Composable
fun SurvivalGuideReaderContent(
    route: Route.SurvivalGuideReader,
    canPrint: Boolean = false,
    onPrint: (Chapter) -> Unit = {}
) {
    val chapter = remember(route.chapter) { Chapters.getChapters().firstOrNull { it.resource == route.chapter } }
    if (chapter == null) {
        val navigator = LocalNavigator.current
        LaunchedEffect(route.chapter) { navigator.popBackStack() }
        return
    }
    val content by produceState<String?>(null, route.chapter) {
        value = withContext(ioDispatcher) { getAppService<RawTextReader>().read(route.chapter) }
    }
    SurvivalGuideReaderScreen(chapter, content, route.headerIndex, canPrint, onPrint)
}
