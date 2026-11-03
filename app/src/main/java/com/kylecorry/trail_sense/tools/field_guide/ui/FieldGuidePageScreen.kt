@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
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
    FieldGuidePageScreen(
        page = controller.page,
        files = controller.files,
        tagNameMapper = controller.tagNameMapper,
        formatter = formatter,
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
    onEdit: () -> Unit,
    onSightings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (page == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FieldGuidePageTopBar(
                    title = AnnotatedString(stringResource(R.string.loading)),
                    canEdit = false,
                    onEdit = onEdit
                )
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(TrailSenseSpacing.lg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            FieldGuidePageContent(
                page = page,
                files = files,
                tagNameMapper = tagNameMapper,
                formatter = formatter,
                onEdit = onEdit,
                onSightings = onSightings
            )
        }
    }
}

/**
 * Slim top bar holding the edit affordance. The prominent title lives in the
 * hero header below so the entry reads like a field-guide card.
 */
@Composable
private fun FieldGuidePageTopBar(
    title: AnnotatedString,
    canEdit: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = TrailSenseSize.maximumContentWidth)
            .fillMaxWidth()
            .padding(start = TrailSenseSpacing.screen, end = TrailSenseSpacing.sm)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .testTag("field_guide_page_title"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (canEdit) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("field_guide_page_edit_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePageContent(
    page: FieldGuidePage,
    files: FileSubsystem,
    tagNameMapper: FieldGuideTagNameMapper,
    formatter: FieldGuideFormatService,
    onEdit: () -> Unit,
    onSightings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FieldGuidePageTopBar(
            title = formatter.formatTitle(page),
            canEdit = !page.isReadOnly,
            onEdit = onEdit
        )

        Column(
            modifier = Modifier
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    bottom = TrailSenseSpacing.xl
                ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            // Hero card: photo + entry name in a tonal primaryContainer surface
            HeroCard(page = page, files = files, formatter = formatter)

            SightingsCard(
                count = page.sightings.size,
                onClick = onSightings
            )

            val notes = page.notes.orEmpty()
            if (notes.isNotBlank()) {
                NotesCard(notes = notes)
            }

            TagsCard(
                tags = page.tags,
                tagNameMapper = tagNameMapper
            )
        }
    }
}

/**
 * Circular tonal icon chip used as a section/leading affordance.
 */
@Composable
private fun TonalIconChip(
    icon: Painter,
    contentDescription: String?
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HeroCard(
    page: FieldGuidePage,
    files: FileSubsystem,
    formatter: FieldGuideFormatService
) {
    val image = page.images.firstOrNull()
    val bitmap = remember(image, files) {
        image?.let { files.bitmap(it, 1000, 500) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("image")
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = formatter.formatTitle(page),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                formatter.formatSubtitle(page)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SightingsCard(
    count: Int,
    onClick: () -> Unit
) {
    TrailSenseCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.sightings),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TonalIconChip(
                icon = painterResource(R.drawable.ic_binoculars),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = count.toString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = stringResource(R.string.sightings_count, count),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TonalIconChip(
                icon = painterResource(R.drawable.ic_tool_notes),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = stringResource(R.string.note),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
        LinkifiedNotes(
            notes = notes,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TagsCard(
    tags: List<FieldGuidePageTag>,
    tagNameMapper: FieldGuideTagNameMapper
) {
    val sortedTags = tags.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))
    if (sortedTags.isEmpty()) {
        return
    }

    TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TonalIconChip(
                icon = painterResource(R.drawable.ic_info),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
        FieldGuideTags(
            tags = sortedTags,
            tagNameMapper = tagNameMapper
        )
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldGuideTags(
    tags: List<FieldGuidePageTag>,
    tagNameMapper: FieldGuideTagNameMapper,
    modifier: Modifier = Modifier
) {
    val sortedTags = tags.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))
    if (sortedTags.isEmpty()) {
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        sortedTags.forEach {
            FieldGuideTagChip(
                tag = it,
                label = tagNameMapper.getName(it)
            )
        }
    }
}

@Composable
private fun FieldGuideTagChip(
    tag: FieldGuidePageTag,
    label: String
) {
    val background = (tagTypeColorMap[tag.type] ?: AppColor.Gray).color
    val foreground = Colors.mostContrastingColor(
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK,
        background
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(background)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = Color(foreground)
        )
    }
}

private val tagTypeColorMap = mapOf(
    FieldGuidePageTagType.Location to AppColor.Gray,
    FieldGuidePageTagType.Habitat to AppColor.Green,
    FieldGuidePageTagType.Classification to AppColor.Blue,
    FieldGuidePageTagType.ActivityPattern to AppColor.Yellow,
    FieldGuidePageTagType.HumanInteraction to AppColor.Brown
)
