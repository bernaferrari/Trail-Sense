package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.FullscreenDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.navigation.ui.LinearCompassView
import kotlinx.coroutines.Dispatchers

class SightingCompassBottomSheetFragment(
    private val onSelect: (bearing: Float?) -> Unit
) : FullscreenDialogFragment() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private var compassViews: SightingCompassViews? = null
    private val binding: SightingCompassViews
        get() = compassViews ?: error("Sighting compass views are not ready")

    var bearing: Float? = null
        set(value) {
            field = value
            if (compassViews != null) {
                updateUI()
            }
        }

    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Default)

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createSightingCompassViews(requireContext())
        compassViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SightingCompassHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            confirmBearing()
            true
        }

        dialog?.setOnCancelListener { onSelect(null) }

        // TODO: Width should always match parent, height can be cropped
        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.start(
            lifecycleOwner = this,
            readFrames = false,
            shouldStabilizePreview = false
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onSelect(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            confirmBearing()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
                leftMargin = insets.left
                rightMargin = insets.right
            }
            windowInsets
        }

        updateUI()
    }

    private fun updateUI() {
        val views = compassViews ?: return
        inBackground {
            fovRunner.enqueue {
                if (compassViews == null) {
                    return@enqueue
                }
                val minimumFOV = 5f
                val range = views.camera.fov.first.coerceAtLeast(minimumFOV)
                onMain {
                    compassViews?.linearCompass?.range = range
                }
            }
        }

        views.toolTitle.title.text = bearing?.let { formatter.formatDegrees(it) } ?: ""
        views.linearCompass.azimuth = bearing ?: 0f
    }

    private fun confirmBearing() {
        onSelect(bearing)
        dismiss()
    }

    override fun onDestroyView() {
        compassViews?.camera?.stop()
        fovRunner.cancel()
        compassViews = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        compassViews?.camera?.stop()
    }
}

private data class SightingCompassViews(
    val root: View,
    val toolTitle: Toolbar,
    val camera: CameraView,
    val linearCompass: LinearCompassView,
    val captureButton: ShutterButton,
    val loadingIndicator: CircularProgressIndicator
)

@Composable
private fun SightingCompassHost(
    views: SightingCompassViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createSightingCompassViews(context: Context): SightingCompassViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.tool_title
        subtitle.isVisible = false
        rightButton.setImageResource(R.drawable.ic_cancel)
        rightButton.isVisible = true
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val camera = CameraView(context, null).apply {
        id = R.id.camera
    }
    val linearCompass = LinearCompassView(context, null).apply {
        id = R.id.linear_compass
    }
    val cameraFrame = FrameLayout(context)
    cameraFrame.addView(
        camera,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    cameraFrame.addView(
        View(context).apply {
            id = R.id.view_camera_line
            setBackgroundColor(Resources.getPrimaryColor(context))
        },
        FrameLayout.LayoutParams(
            Resources.dp(context, 1f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    )
    cameraFrame.addView(
        linearCompass,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Resources.dp(context, 100f).toInt(),
            Gravity.CENTER
        )
    )
    root.addView(
        cameraFrame,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    )

    val captureButton = ShutterButton(context).apply {
        id = R.id.capture_button
        contentDescription = context.getString(R.string.camera_capture)
    }
    val loadingIndicator = CircularProgressIndicator(context).apply {
        id = R.id.loading_indicator
        isIndeterminate = true
        isInvisible = true
    }
    val captureFrame = FrameLayout(context).apply {
        foregroundGravity = Gravity.CENTER
    }
    captureFrame.addView(
        captureButton,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    )
    captureFrame.addView(
        loadingIndicator,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    )
    root.addView(
        captureFrame,
        LinearLayout.LayoutParams(
            Resources.dp(context, 64f).toInt(),
            Resources.dp(context, 64f).toInt()
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = Resources.dp(context, 16f).toInt()
            bottomMargin = Resources.dp(context, 16f).toInt()
        }
    )

    return SightingCompassViews(root, title, camera, linearCompass, captureButton, loadingIndicator)
}
