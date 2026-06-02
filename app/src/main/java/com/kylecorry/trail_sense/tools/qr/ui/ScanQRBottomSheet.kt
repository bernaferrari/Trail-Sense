package com.kylecorry.trail_sense.tools.qr.ui

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class ScanQRBottomSheet(
    private val title: String,
    private val onTextScanned: (text: String?) -> Boolean
) :
    BottomSheetDialogFragment() {

    private val cameraSize = Size(200, 200)
    private val haptics by lazy { HapticSubsystem.getInstance(requireContext()) }
    private var qrImportViews: QRImportViews? = null
    private val binding: QRImportViews?
        get() = qrImportViews

    private var lastMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createQRImportViews(requireContext())
        qrImportViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    QRImportHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return
        views.camera.clipToOutline = true
        views.camera.start(cameraSize) {
            onCameraUpdate(it)
        }

        views.toolTitle.title.text = title

        views.toolTitle.rightButton.setOnClickListener {
            onTextScanned(null)
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding?.camera?.stop()
        qrImportViews = null
        super.onDestroyView()
    }

    private fun onCameraUpdate(bitmap: Bitmap) {
        if (binding == null) {
            bitmap.recycle()
            return
        }
        var message: String? = null
        tryOrNothing {
            message = QR.decode(bitmap)
            bitmap.recycle()
        }
        if (message != null && lastMessage != message) {
            haptics.click()
            lastMessage = message
            if (!onTextScanned(message)) {
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding?.camera?.stop()
        haptics.off()
    }

}

private data class QRImportViews(
    val root: View,
    val toolTitle: Toolbar,
    val camera: CameraView
)

@Composable
private fun QRImportHost(
    views: QRImportViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createQRImportViews(context: Context): QRImportViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.tool_title
        this.title.text = context.getString(R.string.beacon_qr_import_instructions)
        subtitle.isVisible = false
        rightButton.setImageResource(R.drawable.ic_cancel)
        rightButton.isVisible = true
    }
    root.addView(
        title,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val camera = CameraView(context, null).apply {
        id = R.id.camera
        setBackgroundResource(R.drawable.rounded_rectangle)
        setFlipEnabled(true)
    }
    root.addView(
        camera,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.tool_title
            dimensionRatio = "H,1:1"
        }
    )

    return QRImportViews(root, title, camera)
}

@Preview(showBackground = true)
@Composable
private fun QRImportHostPreview() {
    TrailSenseTheme {
        QRImportHost(
            views = createQRImportViews(LocalContext.current)
        )
    }
}
