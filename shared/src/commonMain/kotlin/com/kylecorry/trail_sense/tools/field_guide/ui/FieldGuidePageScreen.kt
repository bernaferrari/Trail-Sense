@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.appPluralString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.text.withWebLinks
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.tools.guide.ui.articleBodyStyle
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.images.rememberAppImage
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuidePageToolScreen(route: Route.FieldGuidePage) {
    val navigator = LocalNavigator.current
    val viewModel = viewModel {
        FieldGuidePageViewModel(route.pageId.takeIf { it != 0L }, getAppService<IFieldGuideRepo>())
    }
    val tagNameMapper = remember { FieldGuideTagNameMapper() }

    LifecycleResumeEffect(viewModel) {
        viewModel.loadPage()
        onPauseOrDispose { }
    }

    val formatter = remember {
        FieldGuideFormatService(getAppService<UserPreferences>())
    }
    val dispatcher = rememberSystemBack()
    FieldGuidePageScreen(
        page = viewModel.page,
        tagNameMapper = tagNameMapper,
        formatter = formatter,
        onBack = { dispatcher?.invoke() },
        onEdit = {
            viewModel.page?.let { navigator.navigate(Route.CreateFieldGuidePage(pageId = it.id)) }
        },
        onSightings = {
            viewModel.page?.let { navigator.navigate(Route.SightingList(pageId = it.id)) }
        }
    )
}

/** One field guide page, reread each time it's shown so edits and new sightings appear. */
class FieldGuidePageViewModel(
    private val pageId: Long?,
    private val repo: IFieldGuideRepo
) : ViewModel() {

    var page by mutableStateOf<FieldGuidePage?>(null)
        private set

    fun loadPage() {
        viewModelScope.launch {
            page = withContext(ioDispatcher) {
                pageId?.let { repo.getPage(it) }
            }
        }
    }
}

@Composable
private fun FieldGuidePageScreen(
    page: FieldGuidePage?,
    tagNameMapper: FieldGuideTagNameMapper,
    formatter: FieldGuideFormatService,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSightings: () -> Unit
) {
    val strings = LocalAppStrings.current
    val dates = remember { FormatService.getInstance() }
    val editLabel = appString(Res.string.edit)

    // One question: what is this, and have I seen it? The photo and its names answer it; sightings and details follow.
    TrailSensePage(
        title = page?.let { formatter.formatTitle(it).text }.orEmpty(),
        onBack = onBack,
        backLabel = appString(Res.string.back),
        actions = {
            if (page != null && !page.isReadOnly) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("field_guide_page_edit_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Edit),
                        contentDescription = editLabel
                    )
                }
            }
        }
    ) {
        // Nothing until the page arrives: the title fills in with the content instead of flashing a placeholder.
        if (page == null) {
            return@TrailSensePage
        }

        val classification = page.tags
            .filter { it.type == FieldGuidePageTagType.Classification }
            .sortedBy { it.ordinal }
            .map { tagNameMapper.getName(it) }
        val otherName = formatter.formatSubtitle(page)

        if (page.images.isNotEmpty() || otherName != null || classification.isNotEmpty()) {
            item(key = "hero") {
                FieldGuidePageHero(
                    images = page.images,
                    otherName = otherName,
                    classification = classification.joinToString(" · ")
                )
            }
        }

        trailSenseSection(key = "sightings") {
            row { position ->
                val lastSeen = page.sightings.mapNotNull { it.time }.maxOrNull()
                val subtitle = listOfNotNull(
                    appPluralString(Res.plurals.field_guide_sighting_count,
                        page.sightings.size,
                        page.sightings.size
                    ),
                    lastSeen?.let {
                        appString(Res.string.field_guide_last_seen,
                            dates.formatRelativeDateTime(it, includeSeconds = false)
                        )
                    }
                ).joinToString(" · ")
                SkyValueRow(
                    title = appString(Res.string.sightings),
                    subtitle = subtitle,
                    icon = TrailIcons.Binoculars,
                    value = page.sightings.size.toString(),
                    position = position,
                    onClick = onSightings,
                    trailing = {
                        Icon(
                            painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("field_guide_page_sightings")
                )
            }
        }

        val notes = page.notes.orEmpty()
        if (notes.isNotBlank()) {
            trailSenseSection(key = "notes", title = getAppService<AppStrings>().get(Res.string.note)) {
                row { _ ->
                    // Notes read as an article, straight on the page.
                    LinkifiedNotes(
                        notes = notes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TrailSenseSpacing.sm)
                    )
                }
            }
        }

        val detailTypes = listOf(
            FieldGuidePageTagType.Location to Res.string.location,
            FieldGuidePageTagType.Habitat to Res.string.habitat,
            FieldGuidePageTagType.ActivityPattern to Res.string.activity_pattern,
            FieldGuidePageTagType.HumanInteraction to Res.string.human_interaction
        ).mapNotNull { (type, label) ->
            val names = page.tags
                .filter { it.type == type }
                .sortedBy { it.ordinal }
                .map { tagNameMapper.getName(it) }
            if (names.isEmpty()) null else Triple(type, strings.get(label), names.joinToString(", "))
        }

        if (detailTypes.isNotEmpty()) {
            trailSenseSection(key = "details", title = getAppService<AppStrings>().get(Res.string.details)) {
                detailTypes.forEach { (type, label, value) ->
                    row { position ->
                        SkyValueRow(
                            title = value,
                            subtitle = label,
                            icon = when (type) {
                                FieldGuidePageTagType.Location -> TrailIcons.Location
                                FieldGuidePageTagType.Habitat -> TrailIcons.Tree
                                FieldGuidePageTagType.ActivityPattern -> TrailIcons.Sun
                                else -> TrailIcons.Paw
                            },
                            position = position
                        )
                    }
                }
            }
        }
    }
}

/**
 * The page's one key fact up front: its photo, the name it's also known by, and what kind of thing it is. The page
 * title already names it, so the hero doesn't say the name again.
 */
@Composable
private fun FieldGuidePageHero(
    images: List<String>,
    otherName: AnnotatedString?,
    classification: String
) {
    // The photo on the page, then the other name in emphasised type and its classification as one quiet line.
    Column(Modifier.fillMaxWidth()) {
        if (images.isNotEmpty()) {
            Box(Modifier.clip(RoundedCornerShape(TrailSenseShape.hero))) {
                FieldGuideImagePager(images = images)
            }
        }
        val hasFacts = otherName != null || classification.isNotBlank()
        if (hasFacts) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_guide_page_title")
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                if (otherName != null) {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (classification.isNotBlank()) {
                    Text(
                        text = classification,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldGuideImagePager(
    images: List<String>,
) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(images) {
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) { index ->
            FieldGuidePageImage(
                path = images[index],
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (images.size > 1) {
        val page = pagerState.currentPage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appString(Res.string.image_index, page + 1, images.size),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TrailSenseSpacing.lg)
                    .testTag("photo_position"),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
            )
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                enabled = page > 0,
                modifier = Modifier.testTag("previous_photo_button")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Prev),
                    contentDescription = appString(Res.string.previous)
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(images.lastIndex)
                        )
                    }
                },
                enabled = page < images.lastIndex,
                modifier = Modifier.testTag("next_photo_button")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                    contentDescription = appString(Res.string.next)
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePageImage(
    path: String,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberAppImage(path, maxWidth = 1000, maxHeight = 500)

    FieldGuideBitmapImage(
        bitmap = bitmap,
        modifier = modifier.testTag("image")
    )
}

@Composable
private fun LinkifiedNotes(
    notes: String,
    modifier: Modifier = Modifier
) {
    val noteText = remember(notes) { notes.withWebLinks() }
    SelectionContainer(modifier = modifier) {
        TrailSenseLinkedText(
            text = noteText,
            modifier = Modifier.fillMaxWidth(),
            style = articleBodyStyle()
        )
    }
}
