@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.graphics.Bitmap
import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuidePageToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { FieldGuidePageController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.loadPage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        controller.loadPage()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val formatter = remember(host) {
        FieldGuideFormatService(UserPreferences(host.requireContext()))
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    FieldGuidePageScreen(
        page = controller.page,
        files = controller.files,
        tagNameMapper = controller.tagNameMapper,
        formatter = formatter,
        onBack = { dispatcher?.onBackPressed() },
        onEdit = controller::editPage,
        onSightings = controller::openSightings
    )
}

private class FieldGuidePageController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    val files by lazy { FileSubsystem.getInstance(host.requireContext()) }
    val tagNameMapper by lazy { FieldGuideTagNameMapper(host.requireContext()) }
    private val pageId: Long? by lazy {
        host.destinationArguments?.getLong("page_id")?.takeIf { it != 0L }
    }

    var page by mutableStateOf<FieldGuidePage?>(null)
        private set

    fun loadPage() {
        host.lifecycleScope.launch {
            page = withContext(Dispatchers.IO) {
                pageId?.let { repo.getPage(it) }
            }
        }
    }

    fun editPage() {
        val current = page ?: return
        host.findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", current.id)
            }
        )
    }

    fun openSightings() {
        val current = page ?: return
        host.findNavController().navigate(
            R.id.sightingListFragment,
            Bundle().apply {
                putLong("page_id", current.id)
            }
        )
    }
}

@Composable
private fun FieldGuidePageScreen(
    page: FieldGuidePage?,
    files: FileSubsystem,
    tagNameMapper: FieldGuideTagNameMapper,
    formatter: FieldGuideFormatService,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSightings: () -> Unit
) {
    val context = LocalContext.current
    val dates = remember(context) { FormatService.getInstance(context) }
    val editLabel = stringResource(R.string.edit)

    TrailSensePage(
        title = page?.let { formatter.formatTitle(it).text }.orEmpty(),
        onBack = onBack,
        backLabel = stringResource(R.string.back),
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
                    files = files,
                    otherName = otherName,
                    classification = classification.joinToString(" · ")
                )
            }
        }

        trailSenseSection(key = "sightings") {
            row { position ->
                val lastSeen = page.sightings.mapNotNull { it.time }.maxOrNull()
                val subtitle = listOfNotNull(
                    pluralStringResource(
                        R.plurals.field_guide_sighting_count,
                        page.sightings.size,
                        page.sightings.size
                    ),
                    lastSeen?.let {
                        stringResource(
                            R.string.field_guide_last_seen,
                            dates.formatRelativeDateTime(it.toZonedDateTime(), includeSeconds = false)
                        )
                    }
                ).joinToString(" · ")
                TrailSenseListItem(
                    title = stringResource(R.string.sightings),
                    subtitle = subtitle,
                    icon = rememberVectorPainter(TrailIcons.Binoculars),
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
            trailSenseSection(key = "notes", title = context.getString(R.string.note)) {
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        LinkifiedNotes(
                            notes = notes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        val detailTypes = listOf(
            FieldGuidePageTagType.Location to R.string.location,
            FieldGuidePageTagType.Habitat to R.string.habitat,
            FieldGuidePageTagType.ActivityPattern to R.string.activity_pattern,
            FieldGuidePageTagType.HumanInteraction to R.string.human_interaction
        ).mapNotNull { (type, label) ->
            val names = page.tags
                .filter { it.type == type }
                .sortedBy { it.ordinal }
                .map { tagNameMapper.getName(it) }
            if (names.isEmpty()) null else context.getString(label) to names.joinToString(", ")
        }

        if (detailTypes.isNotEmpty()) {
            trailSenseSection(key = "details", title = context.getString(R.string.details)) {
                detailTypes.forEach { (label, value) ->
                    row { position ->
                        TrailSenseListItem(
                            title = value,
                            subtitle = label,
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
    files: FileSubsystem,
    otherName: AnnotatedString?,
    classification: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column {
            if (images.isNotEmpty()) {
                FieldGuideImagePager(images = images, files = files)
            }
            val hasFacts = otherName != null || classification.isNotBlank()
            if (hasFacts) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_guide_page_title")
                        .semantics(mergeDescendants = true) {}
                        .padding(
                            horizontal = TrailSenseSpacing.xl,
                            vertical = TrailSenseSpacing.lg
                        ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    if (otherName != null) {
                        Text(
                            text = otherName,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    if (classification.isNotBlank()) {
                        Text(
                            text = classification,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldGuideImagePager(
    images: List<String>,
    files: FileSubsystem
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
                files = files,
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
                text = stringResource(R.string.image_index, page + 1, images.size),
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
                    contentDescription = stringResource(R.string.previous)
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
                    contentDescription = stringResource(R.string.next)
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePageImage(
    path: String,
    files: FileSubsystem,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(path, files) {
        bitmap = withContext(Dispatchers.IO) {
            files.bitmap(path, 1000, 500)
        }
    }

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
    val noteText = remember(notes) {
        val noteText = notes.toSpannable()
        LinkifyCompat.addLinks(noteText, WEB_URLS)
        noteText
    }
    SelectionContainer(modifier = modifier) {
        TrailSenseLinkedText(
            text = noteText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
