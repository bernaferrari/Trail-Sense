@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.views.compose.routeAs

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun CreateFieldGuidePageToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CreateFieldGuidePageController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    CreateFieldGuidePageContent(
        page = controller.page,
        photoIndex = controller.photoIndex,
        tagNames = controller.tags.associateWith { controller.getTagName(it) },
        tags = controller.tags,
        onNameChange = { controller.updatePage(controller.page.copy(name = it)) },
        onScientificNameChange = {
            controller.updatePage(controller.page.copy(scientificName = it.ifBlank { null }))
        },
        onNotesChange = { controller.updatePage(controller.page.copy(notes = it)) },
        onTagsChange = controller::updateTags,
        onTakePhoto = controller::takePhoto,
        onSelectPhoto = controller::selectPhoto,
        onSetDefaultPhoto = controller::setDefaultPhoto,
        onDeleteImage = controller::deleteImage,
        onSave = controller::save,
        modifier = Modifier.fillMaxSize(),
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class CreateFieldGuidePageController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(host.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(host.requireContext()) }
    private val uriPicker by lazy {
        IntentUriPicker(
            host.intentResultRetriever,
            host.requireContext()
        )
    }

    private var isInitialized = false

    private var originalPage by mutableStateOf(FieldGuidePage(0))
    var page by mutableStateOf(originalPage)
        private set
    var photoIndex by mutableStateOf(0)
        private set
    private var backCallback: OnBackPressedCallback? = null

    val tags =
        FieldGuidePageTag.entries.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        host.routeAs<Route.CreateFieldGuidePage>().let {
            val pageId = it.pageId
            if (pageId != 0L) {
                host.inBackground {
                    repo.getPage(pageId)?.let { loaded ->
                        onMain {
                            originalPage = loaded
                            page = loaded
                            photoIndex = 0
                        }
                    }
                }
            }

            val tag = FieldGuidePageTag.entries.withId(it.classificationId)
            if (tag != null) {
                updatePage(page.copy(directTags = page.directTags + tag))
            }
        }
        backCallback = host.promptIfUnsavedChanges(this::hasChanges)
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
    }

    fun getTagName(tag: FieldGuidePageTag): String {
        return tagNameMapper.getName(tag)
    }

    fun updatePage(updated: FieldGuidePage) {
        page = updated
    }

    fun updateTags(type: FieldGuidePageTagType, selected: List<FieldGuidePageTag>) {
        updatePage(page.copy(directTags = page.directTags.filter { it.type != type } + selected))
    }

    fun save() {
        host.inBackground {
            repo.add(page)
            onMain {
                backCallback?.remove()
                host.findNavigator().navigateUp()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return originalPage != page
    }

    fun takePhoto() {
        host.requestCamera {
            if (!it) {
                return@requestCamera
            }
            showPhotoImport()
        }
    }

    fun selectPhoto() {
        host.inBackground(BackgroundMinimumState.Created) {
            val uri = uriPicker.open(listOf("image/*")).getOrNull()
            uploadPhoto(uri)
        }
    }

    fun deleteImage(position: Int) {
        if (position !in page.images.indices) {
            return
        }
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete_image_prompt)
        ) { cancelled ->
            if (!cancelled) {
                val images = page.images.filterIndexed { index, _ -> index != position }
                onPhotosChanged(images, position.coerceAtMost(maxOf(images.lastIndex, 0)))
            }
        }
    }

    fun setDefaultPhoto(position: Int) {
        if (position !in page.images.indices || position == 0) {
            return
        }
        val images = page.images.toMutableList()
        images.add(0, images.removeAt(position))
        onPhotosChanged(images, 0)
    }

    private fun onPhotosChanged(images: List<String>, index: Int) {
        host.inBackground {
            onIO { deleteUnusedPhotos(images) }
            onMain {
                photoIndex = index
                page = page.copy(images = images)
            }
        }
    }

    private fun deleteUnusedPhotos(newImages: List<String>) {
        page.images.filter { it !in newImages && it !in originalPage.images }
            .forEach { files.delete(it) }
    }

    private fun showPhotoImport() {
        val prefs = UserPreferences(host.requireContext()).camera
        host.showPhotoImport(null, prefs.useZeroShutterLag) { uri ->
            host.inBackground {
                uploadPhoto(uri)
            }
        }
    }

    private suspend fun uploadPhoto(uri: Uri?) = onIO {
        uri ?: return@onIO

        val file = files.copyToLocal(uri, "field_guide", "${UUID.randomUUID()}.webp") ?: return@onIO
        val path = files.getLocalPath(file)

        var rotation = 0
        tryOrLog {
            rotation = ExifInterface(file).rotationDegrees
        }
        val bmp = files.bitmap(path, Size(500, 500)) ?: return@onIO
        val rotated = if (rotation != 0) {
            bmp.rotate(rotation.toFloat())
        } else {
            bmp
        }

        if (rotated != bmp) {
            bmp.recycle()
        }

        files.save(path, rotated, 75, true)

        onMain {
            val images = page.images + path
            onPhotosChanged(images, images.lastIndex)
        }
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_CLASSIFICATION_ID = "classification_id"
    }
}

@Composable
private fun CreateFieldGuidePageContent(
    page: FieldGuidePage,
    photoIndex: Int,
    tagNames: Map<FieldGuidePageTag, String>,
    tags: List<FieldGuidePageTag>,
    onNameChange: (String) -> Unit,
    onScientificNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (FieldGuidePageTagType, List<FieldGuidePageTag>) -> Unit,
    onTakePhoto: () -> Unit,
    onSelectPhoto: () -> Unit,
    onSetDefaultPhoto: (Int) -> Unit,
    onDeleteImage: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val saveLabel = stringResource(R.string.save)
    val detailsTitle = stringResource(R.string.details)
    val photoTitle = stringResource(R.string.map_type_photo)
    val notesTitle = stringResource(R.string.tool_notes_title)
    val nameLabel = stringResource(R.string.name)
    val scientificNameLabel = stringResource(R.string.scientific_name)
    val tagSelectors = listOf(
        Triple(FieldGuidePageTagType.Classification, R.string.classification, "tag_classifications"),
        Triple(FieldGuidePageTagType.Location, R.string.location, "tag_locations"),
        Triple(FieldGuidePageTagType.Habitat, R.string.habitat, "tag_habitats"),
        Triple(FieldGuidePageTagType.HumanInteraction, R.string.human_interaction, "tag_human_interactions"),
        Triple(FieldGuidePageTagType.ActivityPattern, R.string.activity_pattern, "tag_activity_patterns")
    ).map { (type, label, tag) -> Triple(type, stringResource(label), tag) }

    Box(modifier = modifier.testTag("scroll_view")) {
        TrailSensePage(
            title = stringResource(R.string.create_page),
            onBack = onBack,
            backLabel = stringResource(R.string.back),
            modifier = Modifier.testTag("create_field_guide_page_title"),
            actions = {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("toolbar_right_button")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Check),
                        contentDescription = saveLabel,
                        modifier = Modifier.testTag("create_field_guide_page_save_btn")
                    )
                }
            }
        ) {
            // The photo leads, as it does on the page itself.
            trailSenseSection(key = "photos", title = photoTitle) {
                row { position ->
                    FieldGuidePhotos(
                        images = page.images,
                        photoIndex = photoIndex,
                        position = position,
                        onTakePhoto = onTakePhoto,
                        onSelectPhoto = onSelectPhoto,
                        onSetDefaultPhoto = onSetDefaultPhoto,
                        onDeleteImage = onDeleteImage
                    )
                }
            }

            trailSenseSection(key = "names") {
                row { position ->
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                        value = page.name,
                        onValueChange = onNameChange,
                        label = nameLabel,
                        position = position,
                        modifier = Modifier.testTag("name"),
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                }
                row { position ->
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                        value = page.scientificName.orEmpty(),
                        onValueChange = onScientificNameChange,
                        label = scientificNameLabel,
                        position = position,
                        modifier = Modifier.testTag("scientific_name"),
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                }
            }

            trailSenseSection(key = "tags", title = detailsTitle) {
                tagSelectors.forEach { (type, label, testTag) ->
                    row { position ->
                        TagSelector(
                            label = label,
                            type = type,
                            tags = tags,
                            selected = page.directTags,
                            tagNames = tagNames,
                            position = position,
                            onSelectionChange = onTagsChange,
                            modifier = Modifier.testTag(testTag)
                        )
                    }
                }
            }

            trailSenseSection(key = "notes", title = notesTitle) {
                row { position ->
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                        value = page.notes.orEmpty(),
                        onValueChange = onNotesChange,
                        label = notesTitle,
                        position = position,
                        singleLine = false,
                        minLines = 6,
                        modifier = Modifier.testTag("notes"),
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                }
            }

            item(key = "save") {
                TrailSenseButton(
                    text = saveLabel,
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FieldGuidePhotos(
    images: List<String>,
    photoIndex: Int,
    position: TrailSenseGroupPosition,
    onTakePhoto: () -> Unit,
    onSelectPhoto: () -> Unit,
    onSetDefaultPhoto: (Int) -> Unit,
    onDeleteImage: (Int) -> Unit
) {
    val pageCount = images.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    LaunchedEffect(images, photoIndex) {
        val target = photoIndex.coerceIn(0, images.size)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    TrailSenseGroupCard(
        position = position,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) { index ->
            val path = images.getOrNull(index)
            if (path == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(TrailSenseSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(
                        TrailSenseSpacing.md,
                        Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PhotoSourceButtons(onTakePhoto = onTakePhoto, onSelectPhoto = onSelectPhoto)
                }
            } else {
                FieldGuideImage(
                    path = path,
                    canSetDefault = index > 0,
                    onSetDefault = { onSetDefaultPhoto(index) },
                    onDelete = { onDeleteImage(index) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (images.isNotEmpty()) {
            val onAddPage = pagerState.currentPage >= images.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (onAddPage) {
                        ""
                    } else {
                        stringResource(
                            R.string.image_index,
                            pagerState.currentPage + 1,
                            images.size
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TrailSenseSpacing.sm)
                        .then(if (onAddPage) Modifier else Modifier.testTag("photo_position")),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    enabled = pagerState.currentPage > 0,
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
                                (pagerState.currentPage + 1).coerceAtMost(images.size)
                            )
                        }
                    },
                    enabled = pagerState.currentPage < images.size,
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
}

@Composable
private fun PhotoSourceButtons(
    onTakePhoto: () -> Unit,
    onSelectPhoto: () -> Unit
) {
    TrailSenseButton(
        text = stringResource(R.string.take_photo),
        onClick = onTakePhoto,
        icon = TrailIcons.Camera,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("take_photo_button")
    )
    TrailSenseButton(
        text = stringResource(R.string.select_from_gallery),
        onClick = onSelectPhoto,
        style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("select_photo_button")
    )
}

@Composable
private fun FieldGuideImage(
    path: String,
    canSetDefault: Boolean,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val files = remember(context) { FileSubsystem.getInstance(context) }
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var menuOpen by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            files.bitmap(path, Size(500, 500))
        }
    }

    Box(modifier = modifier) {
        FieldGuideBitmapImage(
            bitmap = bitmap,
            modifier = Modifier.testTag("image")
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(TrailSenseSpacing.sm)
        ) {
            FilledIconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("photo_upload_menu_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.field_guide_more_options)
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                if (canSetDefault) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.set_as_default_photo)) },
                        onClick = {
                            menuOpen = false
                            onSetDefault()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun TagSelector(
    label: String,
    type: FieldGuidePageTagType,
    tags: List<FieldGuidePageTag>,
    selected: List<FieldGuidePageTag>,
    tagNames: Map<FieldGuidePageTag, String>,
    position: TrailSenseGroupPosition,
    onSelectionChange: (FieldGuidePageTagType, List<FieldGuidePageTag>) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(tags, type) { tags.filter { it.type == type } }
    val selectedOfType = selected.filter { it.type == type }
    val text = selectedOfType.joinToString(", ") { tagNames[it] ?: it.name }
    var isOpen by remember { mutableStateOf(false) }

    TrailSenseListItem(
        title = label,
        subtitle = text.ifBlank { stringResource(R.string.none) },
        position = position,
        onClick = { isOpen = true },
        modifier = modifier,
        trailing = {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )

    if (isOpen) {
        TagSelectionDialog(
            title = label,
            options = options,
            selected = selectedOfType,
            tagNames = tagNames,
            onDismiss = { isOpen = false },
            onConfirm = {
                isOpen = false
                onSelectionChange(type, it)
            }
        )
    }
}

@Composable
private fun TagSelectionDialog(
    title: String,
    options: List<FieldGuidePageTag>,
    selected: List<FieldGuidePageTag>,
    tagNames: Map<FieldGuidePageTag, String>,
    onDismiss: () -> Unit,
    onConfirm: (List<FieldGuidePageTag>) -> Unit
) {
    var current by remember(selected) { mutableStateOf(selected.toSet()) }
    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(options.filter { it in current }) },
        dismissText = stringResource(android.R.string.cancel)
) {
        Column(
            modifier = Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { tag ->
                val checked = tag in current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TrailSenseSize.minimumTouchTarget)
                        .toggleable(
                            value = checked,
                            role = Role.Checkbox,
                            onValueChange = { isChecked ->
                                current = if (isChecked) current + tag else current - tag
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null
                    )
                    Text(
                        text = tagNames[tag] ?: tag.name,
                        modifier = Modifier.padding(start = TrailSenseSpacing.md),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateFieldGuidePageContentPreview() {
    val tagNames = FieldGuidePageTag.entries.associateWith { it.name }
    TrailSenseTheme {
        CreateFieldGuidePageContent(
            page = FieldGuidePage(0, name = "A Test", notes = "Notes"),
            photoIndex = 0,
            tagNames = tagNames,
            tags = FieldGuidePageTag.entries,
            onNameChange = {},
            onScientificNameChange = {},
            onNotesChange = {},
            onTagsChange = { _, _ -> },
            onTakePhoto = {},
            onSelectPhoto = {},
            onSetDefaultPhoto = {},
            onDeleteImage = {},
            onSave = {}
        )
    }
}
