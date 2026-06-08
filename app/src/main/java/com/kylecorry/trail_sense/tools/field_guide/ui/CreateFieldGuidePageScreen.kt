@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.camera.PhotoImportDialog
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CreateFieldGuidePageToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { CreateFieldGuidePageController(fragment) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    CreateFieldGuidePageContent(
        page = controller.page,
        image = controller.imagePreview,
        tagNames = controller.tags.associateWith { controller.getTagName(it) },
        tags = controller.tags,
        onNameChange = { controller.updatePage(controller.page.copy(name = it)) },
        onNotesChange = { controller.updatePage(controller.page.copy(notes = it)) },
        onTagsChange = controller::updateTags,
        onTakePhoto = controller::takePhoto,
        onSelectPhoto = controller::selectPhoto,
        onDeleteImage = controller::deleteImage,
        onSave = controller::save,
        modifier = Modifier.fillMaxSize()
    )
    controller.PhotoImportContent()
}

private class CreateFieldGuidePageController(private val fragment: Fragment) {

    private val repo by lazy { FieldGuideRepo.getInstance(fragment.requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(fragment.requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(fragment.requireContext()) }
    private val uriPicker by lazy {
        IntentUriPicker(
            fragment.requireActivity() as IntentResultRetriever,
            fragment.requireContext()
        )
    }

    private var isInitialized = false

    private var originalPage by mutableStateOf(FieldGuidePage(0))
    var page by mutableStateOf(originalPage)
        private set
    var imagePreview by mutableStateOf<Bitmap?>(null)
        private set
    private var photoImportDialog by mutableStateOf<PhotoImportDialog?>(null)
    private var backCallback: OnBackPressedCallback? = null

    val tags =
        FieldGuidePageTag.entries.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        fragment.arguments?.let {
            val pageId = it.getLong(ARG_PAGE_ID, 0L)
            if (pageId != 0L) {
                fragment.inBackground {
                    repo.getPage(pageId)?.let { loaded ->
                        onMain {
                            originalPage = loaded
                            updatePage(loaded)
                        }
                    }
                }
            }

            val tag = FieldGuidePageTag.entries.withId(it.getLong(ARG_CLASSIFICATION_ID, 0L))
            if (tag != null) {
                updatePage(page.copy(directTags = page.directTags + tag))
            }
        }
        backCallback = fragment.promptIfUnsavedChanges(this::hasChanges)
        refreshImagePreview()
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
        imagePreview = null
        photoImportDialog?.dismiss()
        photoImportDialog = null
    }

    @Composable
    fun PhotoImportContent() {
        photoImportDialog?.Content()
    }

    fun getTagName(tag: FieldGuidePageTag): String {
        return tagNameMapper.getName(tag)
    }

    fun updatePage(updated: FieldGuidePage) {
        val oldImage = page.images.firstOrNull()
        page = updated
        if (oldImage != updated.images.firstOrNull()) {
            refreshImagePreview()
        }
    }

    fun updateTags(type: FieldGuidePageTagType, selected: List<FieldGuidePageTag>) {
        updatePage(page.copy(directTags = page.directTags.filter { it.type != type } + selected))
    }

    fun save() {
        fragment.inBackground {
            repo.add(page)
            onMain {
                backCallback?.remove()
                fragment.findNavController().navigateUp()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return originalPage != page
    }

    fun takePhoto() {
        fragment.requestCamera {
            if (!it) {
                return@requestCamera
            }
            fragment.inBackground {
                val uri = takePhoto(fragment)
                uploadPhoto(uri)
            }
        }
    }

    fun selectPhoto() {
        fragment.inBackground(BackgroundMinimumState.Created) {
            val uri = uriPicker.open(listOf("image/*"))
            uploadPhoto(uri)
        }
    }

    fun deleteImage() {
        fragment.inBackground {
            deleteImageInternal()
        }
    }

    private fun refreshImagePreview() {
        val image = page.images.firstOrNull()
        if (image == null) {
            imagePreview = null
            return
        }

        fragment.inBackground {
            val bitmap = onIO { files.bitmap(image, Size(500, 500)) }
            onMain { imagePreview = bitmap }
        }
    }

    private suspend fun takePhoto(fragment: Fragment): Uri? {
        return suspendCoroutine { cont ->
            val prefs = UserPreferences(fragment.requireContext()).camera
            fragment.requireActivity().runOnUiThread {
                photoImportDialog = PhotoImportDialog(
                    fragment.requireContext(),
                    fragment.viewLifecycleOwner,
                    fragment.viewLifecycleOwner.lifecycleScope,
                    null,
                    prefs.useZeroShutterLag
                ) { uri ->
                    photoImportDialog = null
                    cont.resume(uri)
                }.also {
                    it.show()
                }
            }
        }
    }

    private suspend fun uploadPhoto(uri: Uri?) = onIO {
        uri ?: return@onIO

        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }

        val file = files.copyToLocal(uri, "field_guide", "${UUID.randomUUID()}.webp") ?: return@onIO
        val path = files.getLocalPath(file)

        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(uri.toFile())
            rotation = exif.rotationDegrees
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
            updatePage(page.copy(images = listOf(path)))
        }
    }

    private suspend fun deleteImageInternal() = onIO {
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }
        onMain {
            updatePage(page.copy(images = emptyList()))
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
    image: Bitmap?,
    tagNames: Map<FieldGuidePageTag, String>,
    tags: List<FieldGuidePageTag>,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (FieldGuidePageTagType, List<FieldGuidePageTag>) -> Unit,
    onTakePhoto: () -> Unit,
    onSelectPhoto: () -> Unit,
    onDeleteImage: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateFieldGuidePageHeader(onSave = onSave)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("scroll_view")
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(bottom = TrailSenseSpacing.xl)
            ) {
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                OutlinedTextField(
                    value = page.name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name"),
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                TagSelector(
                    label = stringResource(R.string.classification),
                    type = FieldGuidePageTagType.Classification,
                    tags = tags,
                    selected = page.directTags,
                    tagNames = tagNames,
                    onSelectionChange = onTagsChange,
                    modifier = Modifier.testTag("tag_classifications")
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                if (image != null) {
                    FieldGuideImage(
                        image = image,
                        onDelete = onDeleteImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("image")
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    Button(
                        onClick = onTakePhoto,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("take_photo_button")
                    ) {
                        Text(stringResource(R.string.take_photo))
                    }
                    Button(
                        onClick = onSelectPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_photo_button")
                    ) {
                        Text(stringResource(R.string.select_from_gallery))
                    }
                }
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                OutlinedTextField(
                    value = page.notes.orEmpty(),
                    onValueChange = onNotesChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("notes"),
                    label = { Text(stringResource(R.string.tool_notes_title)) }
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                TagSelector(
                    label = stringResource(R.string.location),
                    type = FieldGuidePageTagType.Location,
                    tags = tags,
                    selected = page.directTags,
                    tagNames = tagNames,
                    onSelectionChange = onTagsChange,
                    modifier = Modifier.testTag("tag_locations")
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                TagSelector(
                    label = stringResource(R.string.habitat),
                    type = FieldGuidePageTagType.Habitat,
                    tags = tags,
                    selected = page.directTags,
                    tagNames = tagNames,
                    onSelectionChange = onTagsChange,
                    modifier = Modifier.testTag("tag_habitats")
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                TagSelector(
                    label = stringResource(R.string.human_interaction),
                    type = FieldGuidePageTagType.HumanInteraction,
                    tags = tags,
                    selected = page.directTags,
                    tagNames = tagNames,
                    onSelectionChange = onTagsChange,
                    modifier = Modifier.testTag("tag_human_interactions")
                )
                Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                TagSelector(
                    label = stringResource(R.string.activity_pattern),
                    type = FieldGuidePageTagType.ActivityPattern,
                    tags = tags,
                    selected = page.directTags,
                    tagNames = tagNames,
                    onSelectionChange = onTagsChange,
                    modifier = Modifier.testTag("tag_activity_patterns")
                )
            }
        }
    }
}

@Composable
private fun CreateFieldGuidePageHeader(
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.sm)
            .padding(vertical = TrailSenseSpacing.sm)
            .testTag("create_field_guide_page_title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.create_page),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onSave,
            modifier = Modifier.testTag("create_field_guide_page_save_btn")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(android.R.string.ok)
            )
        }
    }
}

@Composable
private fun FieldGuideImage(
    image: Bitmap,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("delete_image_button")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cancel),
                contentDescription = stringResource(android.R.string.cancel)
            )
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
    onSelectionChange: (FieldGuidePageTagType, List<FieldGuidePageTag>) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(tags, type) { tags.filter { it.type == type } }
    val selectedOfType = selected.filter { it.type == type }
    val text = selectedOfType.joinToString(", ") { tagNames[it] ?: it.name }
    var isOpen by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable { isOpen = true },
        label = { Text(label) },
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                current = if (tag in current) {
                                    current - tag
                                } else {
                                    current + tag
                                }
                            }
                            .padding(vertical = TrailSenseSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tag in current,
                            onCheckedChange = { checked ->
                                current = if (checked) current + tag else current - tag
                            }
                        )
                        Text(
                            text = tagNames[tag] ?: tag.name,
                            modifier = Modifier.padding(start = TrailSenseSpacing.sm),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(options.filter { it in current }) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateFieldGuidePageContentPreview() {
    val tagNames = FieldGuidePageTag.entries.associateWith { it.name }
    TrailSenseTheme {
        CreateFieldGuidePageContent(
            page = FieldGuidePage(0, name = "A Test", notes = "Notes"),
            image = null,
            tagNames = tagNames,
            tags = FieldGuidePageTag.entries,
            onNameChange = {},
            onNotesChange = {},
            onTagsChange = { _, _ -> },
            onTakePhoto = {},
            onSelectPhoto = {},
            onDeleteImage = {},
            onSave = {}
        )
    }
}
