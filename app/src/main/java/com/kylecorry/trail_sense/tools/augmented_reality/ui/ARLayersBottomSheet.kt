package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.replaceChildFragment
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class ARLayersBottomSheet : BottomSheetDialogFragment() {

    private var onDismissListener: (() -> Unit)? = null
    private var arLayerViews: ARLayerViews? = null
    private val binding: ARLayerViews
        get() = arLayerViews ?: error("AR layer views are not ready")

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createARLayerViews(requireContext())
        arLayerViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    ARLayerHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        arLayerViews = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.rightButton.setOnClickListener {
            dismiss()
        }

        replaceChildFragment(
            ARLayersBottomSheetPreferenceFragment(),
            binding.preferencesFragment.id
        )
    }
}

private data class ARLayerViews(
    val root: View,
    val title: Toolbar,
    val overrideReason: TextView,
    val preferencesFragment: FragmentContainerView
)

@Composable
private fun ARLayerHost(
    views: ARLayerViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createARLayerViews(context: Context): ARLayerViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.title
        this.title.text = context.getString(R.string.layers)
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

    val overrideReason = TextView(context).apply {
        id = R.id.override_reason
        text = context.getString(R.string.ar_layer_override_message)
    }
    root.addView(
        overrideReason,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val preferences = FragmentContainerView(context).apply {
        id = R.id.preferences_fragment
    }
    root.addView(
        preferences,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    return ARLayerViews(root, title, overrideReason, preferences)
}

@Preview(showBackground = true)
@Composable
private fun ARLayerHostPreview() {
    TrailSenseTheme {
        ARLayerHost(
            views = createARLayerViews(LocalContext.current)
        )
    }
}
