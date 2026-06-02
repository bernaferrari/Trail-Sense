package com.kylecorry.trail_sense.shared.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Size
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
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.FullscreenDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class PhotoImportBottomSheetFragment(
    private val resolution: Size? = null,
    private val useZeroShutterLag: Boolean = false,
    private val onCapture: (uri: Uri?) -> Unit
) : FullscreenDialogFragment() {

    private var isCapturing = false
    private var photoViews: PhotoImportViews? = null
    private val binding: PhotoImportViews
        get() = photoViews ?: error("Photo import views are not ready")

    private var orientation = DeviceOrientation.Orientation.Portrait

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createPhotoImportViews(requireContext())
        photoViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PhotoImportHost(
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

        val orientationSensor = SensorService(requireContext()).getDeviceOrientationSensor()
        orientation = DeviceOrientation.Orientation.Portrait
        observe(orientationSensor) {
            val newOrientation = orientationSensor.orientation
            if (newOrientation == DeviceOrientation.Orientation.Landscape || newOrientation == DeviceOrientation.Orientation.LandscapeInverse) {
                orientation = newOrientation
            } else if (newOrientation == DeviceOrientation.Orientation.Portrait || newOrientation == DeviceOrientation.Orientation.PortraitInverse) {
                orientation = newOrientation
            }
        }

        val volumeKeys = listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (!volumeKeys.contains(keyCode) || event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            takePhoto()
            true
        }

        dialog?.setOnCancelListener { onCapture(null) }

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.start(
            resolution,
            lifecycleOwner = this,
            readFrames = false,
            captureSettings = ImageCaptureSettings(
                captureMode = if (useZeroShutterLag) {
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                } else if (resolution == null) {
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                } else {
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
            )
        )

        binding.toolTitle.rightButton.setOnClickListener {
            onCapture(null)
            dismiss()
        }

        binding.captureButton.setOnClickListener {
            takePhoto()
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
    }

    private fun takePhoto() {
        val views = photoViews ?: return
        if (isCapturing) {
            return
        }
        isCapturing = true
        views.captureButton.isInvisible = true
        views.loadingIndicator.isVisible = true
        inBackground {
            val file = FileSubsystem.getInstance(requireContext()).createTemp("jpg")
            val success = onIO {
                val saved = views.camera.capture(file)

                // Set the orientation EXIF
                tryOrLog {
                    if (orientation != DeviceOrientation.Orientation.Portrait) {
                        val exif = ExifInterface(file)
                        exif.rotate(
                            when (orientation) {
                                DeviceOrientation.Orientation.PortraitInverse -> 180
                                DeviceOrientation.Orientation.Landscape -> 270
                                DeviceOrientation.Orientation.LandscapeInverse -> 90
                                else -> 0
                            }
                        )
                        exif.saveAttributes()
                    }
                }

                saved
            }
            onMain {
                if (photoViews == null) {
                    file.delete()
                    return@onMain
                }
                if (!success) {
                    file.delete()
                    onCapture(null)
                } else {
                    onCapture(file.toUri())
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        photoViews?.camera?.stop()
        photoViews = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        photoViews?.camera?.stop()
    }
}

private data class PhotoImportViews(
    val root: View,
    val toolTitle: Toolbar,
    val camera: CameraView,
    val captureButton: ShutterButton,
    val loadingIndicator: CircularProgressIndicator
)

@Composable
private fun PhotoImportHost(
    views: PhotoImportViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createPhotoImportViews(context: Context): PhotoImportViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.tool_title
        this.title.text = context.getString(R.string.take_photo)
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
        setFlipEnabled(true)
    }
    root.addView(
        camera,
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

    return PhotoImportViews(root, title, camera, captureButton, loadingIndicator)
}
