package com.kylecorry.trail_sense.tools.field_guide.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.widget.NestedScrollView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.MaterialMultiSpinnerView
import com.kylecorry.trail_sense.shared.views.Notepad
import com.kylecorry.trail_sense.shared.views.TextInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import java.util.UUID

class CreateFieldGuidePageFragment : AndromedaFragment() {

    private var createPageViews: CreateFieldGuidePageViews? = null
    private val binding: CreateFieldGuidePageViews
        get() = createPageViews ?: error("Create field guide page views are not ready")
    private val isBound: Boolean
        get() = createPageViews != null

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }
    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }

    private var originalPage = FieldGuidePage(0)
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                updateOriginalContent()
            }
        }
    private var page = originalPage
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                updatePageContent()
            }
        }

    private var backCallback: OnBackPressedCallback? = null

    // Tags
    private val tags =
        FieldGuidePageTag.entries.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createFieldGuidePageViews(requireContext())
        createPageViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    CreateFieldGuidePageHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pageId = it.getLong(ARG_PAGE_ID, 0L)
            if (pageId != 0L) {
                inBackground {
                    repo.getPage(pageId)?.let {
                        originalPage = it
                        page = it
                    }
                }
            }

            val tag = FieldGuidePageTag.entries.withId(it.getLong(ARG_CLASSIFICATION_ID, 0L))

            if (tag != null) {
                page = page.copy(directTags = page.directTags + tag)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CustomUiUtils.setButtonState(binding.createFieldGuidePageTitle.rightButton, true)
        binding.createFieldGuidePageTitle.rightButton.setOnClickListener {
            save()
        }

        // Fields
        binding.name.setOnTextChangeListener {
            page = page.copy(name = it.toString())
        }

        binding.notes.addTextChangedListener {
            page = page.copy(notes = it.toString())
        }

        CustomUiUtils.setButtonState(binding.deleteImageButton, false)
        binding.deleteImageButton.setOnClickListener {
            inBackground {
                deleteImage()
            }
        }

        binding.takePhotoButton.setOnClickListener {
            inBackground {
                val uri = CustomUiUtils.takePhoto(this@CreateFieldGuidePageFragment)
                uploadPhoto(uri)
            }
        }

        binding.selectPhotoButton.setOnClickListener {
            inBackground(BackgroundMinimumState.Created) {
                val uri = uriPicker.open(listOf("image/*"))
                uploadPhoto(uri)
            }
        }

        initializeTags(
            binding.tagLocations,
            getString(R.string.location),
            FieldGuidePageTagType.Location
        )

        initializeTags(
            binding.tagHabitats,
            getString(R.string.habitat),
            FieldGuidePageTagType.Habitat
        )

        initializeTags(
            binding.tagClassifications,
            getString(R.string.classification),
            FieldGuidePageTagType.Classification
        )

        initializeTags(
            binding.tagActivityPatterns,
            getString(R.string.activity_pattern),
            FieldGuidePageTagType.ActivityPattern
        )

        initializeTags(
            binding.tagHumanInteractions,
            getString(R.string.human_interaction),
            FieldGuidePageTagType.HumanInteraction
        )

        backCallback = promptIfUnsavedChanges(this::hasChanges)
        updateOriginalContent()
        updatePageContent()
    }

    override fun onDestroyView() {
        createPageViews = null
        super.onDestroyView()
    }

    private fun updateOriginalContent() {
        if (!isBound) {
            return
        }
        binding.name.text = originalPage.name
        binding.notes.setText(originalPage.notes)
    }

    private fun updatePageContent() {
        if (!isBound) {
            return
        }
        setTags(binding.tagLocations, page.directTags, FieldGuidePageTagType.Location)
        setTags(binding.tagHabitats, page.directTags, FieldGuidePageTagType.Habitat)
        setTags(
            binding.tagClassifications,
            page.directTags,
            FieldGuidePageTagType.Classification
        )
        setTags(
            binding.tagActivityPatterns,
            page.directTags,
            FieldGuidePageTagType.ActivityPattern
        )
        setTags(
            binding.tagHumanInteractions,
            page.directTags,
            FieldGuidePageTagType.HumanInteraction
        )
        val image = page.images.firstOrNull()
        binding.imageHolder.isVisible = image != null
        if (image != null) {
            binding.image.setImageURI(files.uri(image))
        } else {
            binding.image.setImageURI(null)
        }
    }

    private fun save() {
        inBackground {
            repo.add(page)
            onMain {
                backCallback?.remove()
                findNavController().navigateUp()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return originalPage != page
    }

    private suspend fun uploadPhoto(uri: Uri?) = onIO {
        uri ?: return@onIO

        // Delete unsaved images
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }

        // Copy over the new image
        val file = files.copyToLocal(uri, "field_guide", "${UUID.randomUUID()}.webp") ?: return@onIO
        val path = files.getLocalPath(file)

        // Reduce the resolution
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

        // Update the page
        page = page.copy(images = listOf(path))
    }

    private suspend fun deleteImage() = onIO {
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }
        page = page.copy(images = emptyList())
    }

    private fun initializeTags(
        view: MaterialMultiSpinnerView,
        hint: String,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        view.setHint(hint)
        view.setItems(tagsOfType.map { tagNameMapper.getName(it) })
        view.setOnSelectionChangeListener {
            page =
                page.copy(directTags = page.directTags.filter { it.type != type } + it.map { tagsOfType[it] })
        }
    }

    private fun setTags(
        view: MaterialMultiSpinnerView,
        selection: List<FieldGuidePageTag>,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        val selectionOfType = selection.filter { it.type == type }
        view.setSelection(selectionOfType.map { tagsOfType.indexOf(it) })
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_CLASSIFICATION_ID = "classification_id"
    }
}

private data class CreateFieldGuidePageViews(
    val root: View,
    val createFieldGuidePageTitle: CreateFieldGuidePageTitle,
    val scrollView: NestedScrollView,
    val name: TextInputView,
    val tagClassifications: MaterialMultiSpinnerView,
    val imageHolder: FrameLayout,
    val image: ImageView,
    val deleteImageButton: ImageButton,
    val takePhotoButton: MaterialButton,
    val selectPhotoButton: MaterialButton,
    val notes: Notepad,
    val tagLocations: MaterialMultiSpinnerView,
    val tagHabitats: MaterialMultiSpinnerView,
    val tagHumanInteractions: MaterialMultiSpinnerView,
    val tagActivityPatterns: MaterialMultiSpinnerView
)

private data class CreateFieldGuidePageTitle(
    val root: View,
    val rightButton: ImageButton
)

@Composable
private fun CreateFieldGuidePageHost(
    views: CreateFieldGuidePageViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createFieldGuidePageViews(context: Context): CreateFieldGuidePageViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = createFieldGuidePageTitle(context)
    root.addView(
        title.root,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val scrollView = NestedScrollView(context).apply {
        id = R.id.scroll_view
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, 16.dp(context))
    }
    scrollView.addView(
        content,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
    root.addView(
        scrollView,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    )

    val name = TextInputView(context, null).apply {
        id = R.id.name
        setHint(context.getString(R.string.name))
    }
    content.addView(name, fieldParams(context))

    val tagClassifications = MaterialMultiSpinnerView(context, null).apply {
        id = R.id.tag_classifications
    }
    content.addView(tagClassifications, fieldParams(context, top = 16.dp(context)))

    val imageHolder = FrameLayout(context).apply {
        id = R.id.image_holder
        visibility = View.GONE
    }
    val image = ImageView(context).apply {
        id = R.id.image
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    val deleteImageButton = ImageButton(context).apply {
        id = R.id.delete_image_button
        setImageResource(R.drawable.ic_cancel)
        background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(Resources.color(context, R.color.colorSecondary))
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        elevation = 2.dp(context).toFloat()
    }
    imageHolder.addView(
        image,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            200.dp(context)
        )
    )
    imageHolder.addView(
        deleteImageButton,
        FrameLayout.LayoutParams(32.dp(context), 32.dp(context), Gravity.TOP or Gravity.END).apply {
            setMargins(8.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
        }
    )
    content.addView(imageHolder, fieldParams(context, top = 16.dp(context)))

    val photoButtons = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
    }
    val takePhotoButton = MaterialButton(context).apply {
        id = R.id.take_photo_button
        text = context.getString(R.string.take_photo)
    }
    val selectPhotoButton = MaterialButton(context).apply {
        id = R.id.select_photo_button
        text = context.getString(R.string.select_from_gallery)
    }
    photoButtons.addView(
        takePhotoButton,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    photoButtons.addView(
        selectPhotoButton,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = 16.dp(context)
        }
    )
    content.addView(photoButtons, fieldParams(context, top = 16.dp(context)))

    val notesCard = MaterialCardView(context).apply {
        cardElevation = 0f
        setCardBackgroundColor(Color.TRANSPARENT)
    }
    val notes = Notepad(context, null).apply {
        id = R.id.notes
        hint = context.getString(R.string.tool_notes_title)
        imeOptions = EditorInfo.IME_ACTION_NONE
        setLineSpacing(0f, 1.5f)
        setPadding(16.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
        isVerticalScrollBarEnabled = true
    }
    notesCard.addView(
        notes,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    content.addView(notesCard, fieldParams(context, top = 16.dp(context), height = 200.dp(context)))

    val tagLocations = MaterialMultiSpinnerView(context, null).apply {
        id = R.id.tag_locations
    }
    val tagHabitats = MaterialMultiSpinnerView(context, null).apply {
        id = R.id.tag_habitats
    }
    val tagHumanInteractions = MaterialMultiSpinnerView(context, null).apply {
        id = R.id.tag_human_interactions
    }
    val tagActivityPatterns = MaterialMultiSpinnerView(context, null).apply {
        id = R.id.tag_activity_patterns
    }
    content.addView(tagLocations, fieldParams(context, top = 16.dp(context)))
    content.addView(tagHabitats, fieldParams(context, top = 16.dp(context)))
    content.addView(tagHumanInteractions, fieldParams(context, top = 16.dp(context)))
    content.addView(tagActivityPatterns, fieldParams(context, top = 16.dp(context)))

    return CreateFieldGuidePageViews(
        root = root,
        createFieldGuidePageTitle = title,
        scrollView = scrollView,
        name = name,
        tagClassifications = tagClassifications,
        imageHolder = imageHolder,
        image = image,
        deleteImageButton = deleteImageButton,
        takePhotoButton = takePhotoButton,
        selectPhotoButton = selectPhotoButton,
        notes = notes,
        tagLocations = tagLocations,
        tagHabitats = tagHabitats,
        tagHumanInteractions = tagHumanInteractions,
        tagActivityPatterns = tagActivityPatterns
    )
}

private fun createFieldGuidePageTitle(context: Context): CreateFieldGuidePageTitle {
    val root = LinearLayout(context).apply {
        id = R.id.create_field_guide_page_title
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(16.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
    }
    val title = TextView(context).apply {
        text = context.getString(R.string.create_page)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    val save = ImageButton(context).apply {
        id = R.id.create_field_guide_page_save_btn
        setImageResource(R.drawable.ic_check)
        backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    root.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(save, LinearLayout.LayoutParams(48.dp(context), 48.dp(context)))
    return CreateFieldGuidePageTitle(root, save)
}

private fun fieldParams(
    context: Context,
    top: Int = 0,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
): LinearLayout.LayoutParams {
    return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
        setMargins(16.dp(context), top, 16.dp(context), 0)
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun CreateFieldGuidePageHostPreview() {
    TrailSenseTheme {
        CreateFieldGuidePageHost(
            views = createFieldGuidePageViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
