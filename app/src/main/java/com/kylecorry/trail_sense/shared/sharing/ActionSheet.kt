package com.kylecorry.trail_sense.shared.sharing

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.TileButton

class ActionSheet(
    private val title: String,
    private val subtitle: String?,
    private val actions: List<ActionItem>,
    private val onAction: (action: ActionItem?, sheet: ActionSheet) -> Unit
) : BottomSheetDialogFragment() {

    private var actionSheetViews: ActionSheetViews? = null
    private val binding: ActionSheetViews
        get() = actionSheetViews ?: error("Action sheet views are not ready")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createActionSheetViews(requireContext())
        actionSheetViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    ActionSheetHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        actionSheetViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shareSheetTitle.title.text = title
        binding.shareSheetTitle.subtitle.isVisible = subtitle != null
        binding.shareSheetTitle.subtitle.text = subtitle

        // Insert custom view between toolbar and action buttons if provided
        customView?.let { cv ->
            (cv.parent as? ViewGroup)?.removeView(cv)
            val idx = binding.root.indexOfChild(binding.shareSheetItems)
            binding.root.addView(cv, idx)
        }

        actions.forEach { action ->
            val tile = TileButton(requireContext(), null)
            tile.setText(action.name)
            tile.setImageResource(action.icon)
            tile.setTilePadding(Resources.dp(requireContext(), 32f).toInt())
            tile.layoutParams = FlexboxLayout.LayoutParams(
                Resources.dp(requireContext(), 90f).toInt(),
                Resources.dp(requireContext(), 90f).toInt()
            ).also {
                it.setMargins(Resources.dp(requireContext(), 8f).toInt())
            }
            tile.setOnClickListener {
                onAction(action, this)
            }
            binding.shareSheetItems.addView(tile)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onAction(null, this)
    }
}

private data class ActionSheetViews(
    val root: View,
    val shareSheetTitle: Toolbar,
    val shareSheetItems: FlexboxLayout
)

@Composable
private fun ActionSheetHost(
    views: ActionSheetViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createActionSheetViews(context: Context): ActionSheetViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.share_sheet_title
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16.dp(context)
        }
    )

    val items = FlexboxLayout(context).apply {
        id = R.id.share_sheet_items
        alignItems = AlignItems.CENTER
        flexWrap = FlexWrap.WRAP
        justifyContent = JustifyContent.CENTER
    }
    root.addView(
        items,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    return ActionSheetViews(root, title, items)
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun ActionSheetHostPreview() {
    TrailSenseTheme {
        ActionSheetHost(
            views = createActionSheetViews(LocalContext.current)
        )
    }
}
