@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuidePageToolScreen(fragment: TrailSenseDestinationHost) {
    val controller = remember(fragment) { FieldGuidePageController(fragment) }
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

    FieldGuidePageScreen(
        page = controller.page,
        files = controller.files,
        tagNameMapper = controller.tagNameMapper,
        onEdit = controller::editPage,
        onSightings = controller::openSightings
    )
}

private class FieldGuidePageController(private val fragment: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(fragment.requireContext()) }
    val files by lazy { FileSubsystem.getInstance(fragment.requireContext()) }
    val tagNameMapper by lazy { FieldGuideTagNameMapper(fragment.requireContext()) }
    private val pageId: Long? by lazy {
        fragment.destinationArguments?.getLong("page_id")?.takeIf { it != 0L }
    }

    var page by mutableStateOf<FieldGuidePage?>(null)
        private set

    fun loadPage() {
        fragment.lifecycleScope.launch {
            page = withContext(Dispatchers.IO) {
                pageId?.let { repo.getPage(it) }
            }
        }
    }

    fun editPage() {
        val current = page ?: return
        fragment.findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", current.id)
            }
        )
    }

    fun openSightings() {
        val current = page ?: return
        fragment.findNavController().navigate(
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
    onEdit: () -> Unit,
    onSightings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FieldGuidePageHeader(
                title = page?.name ?: stringResource(R.string.loading),
                canEdit = page?.isReadOnly == false,
                onEdit = onEdit
            )
            if (page == null) {
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FieldGuidePageContent(
                    page = page,
                    files = files,
                    tagNameMapper = tagNameMapper,
                    onSightings = onSightings
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePageHeader(
    title: String,
    canEdit: Boolean,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .testTag("field_guide_page_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canEdit) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("field_guide_page_edit_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldGuidePageContent(
    page: FieldGuidePage,
    files: FileSubsystem,
    tagNameMapper: FieldGuideTagNameMapper,
    onSightings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        val image = page.images.firstOrNull()
        if (image != null) {
            val bitmap = remember(image, files) {
                files.bitmap(image, 1000, 500)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .testTag("image")
                )
            }
        }

        Text(
            text = stringResource(R.string.sightings_count, page.sightings.size),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clickable { onSightings() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        val notes = page.notes.orEmpty()
        if (notes.isNotBlank()) {
            LinkifiedNotes(
                notes = notes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        FieldGuideTags(
            tags = page.tags,
            tagNameMapper = tagNameMapper,
            modifier = Modifier.padding(top = 16.dp)
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
            style = MaterialTheme.typography.bodyMedium
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
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
