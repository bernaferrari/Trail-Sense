package com.kylecorry.trail_sense.tools.qr.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class ViewQRBottomSheet(
    private val title: String,
    private val text: String
) : BottomSheetDialogFragment() {

    private var qrViews: QRShareViews? = null
    private val binding: QRShareViews?
        get() = qrViews

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.qrCode?.clipToOutline = true
        updateUI()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createQRShareViews(requireContext())
        qrViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    QRShareHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        qrViews = null
        super.onDestroyView()
    }

    private fun updateUI() {
        val views = binding ?: return
        views.qrTitle.text = title
        val size = Resources.dp(requireContext(), 250f).toInt()

        if (text.length > MAX_LENGTH) {
            toast(getString(R.string.qr_text_too_long))
        }

        val bitmap = QR.encode(text.take(MAX_LENGTH), size, size)
        views.qrCode.setImageBitmap(bitmap)
    }

    companion object {
        const val MAX_LENGTH = 1000
    }

}

private data class QRShareViews(
    val root: View,
    val qrTitle: TextView,
    val qrCode: ImageView
)

@Composable
private fun QRShareHost(
    views: QRShareViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createQRShareViews(context: Context): QRShareViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = TextView(context).apply {
        id = R.id.qr_title
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    )

    val code = ImageView(context).apply {
        id = R.id.qr_code
        setBackgroundResource(R.drawable.rounded_rectangle)
        elevation = 4.dp(context).toFloat()
    }
    root.addView(
        code,
        LinearLayout.LayoutParams(250.dp(context), 250.dp(context)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 16.dp(context)
            bottomMargin = 16.dp(context)
        }
    )

    return QRShareViews(root, title, code)
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun QRShareHostPreview() {
    TrailSenseTheme {
        QRShareHost(
            views = createQRShareViews(LocalContext.current)
        )
    }
}
