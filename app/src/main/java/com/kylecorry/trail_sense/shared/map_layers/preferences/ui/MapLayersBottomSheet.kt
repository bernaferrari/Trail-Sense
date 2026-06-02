package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils.replaceChildFragment
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch

class MapLayersBottomSheet(
    private val mapId: String,
    private val alwaysEnabledLayerIds: List<String> = emptyList()
) : BottomSheetDialogFragment() {

    private var onDismissListener: (() -> Unit)? = null
    private var mapLayerViews: MapLayerViews? = null
    private val binding: MapLayerViews
        get() = mapLayerViews ?: error("Map layer views are not ready")

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createMapLayerViews(requireContext())
        mapLayerViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    MapLayerHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        mapLayerViews = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
        if (mapId == MapToolRegistration.MAP_ID) {
            Tools.triggerWidgetUpdate(requireContext(), MapToolRegistration.WIDGET_MAP)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.rightButton.setOnClickListener {
            dismiss()
        }
        loadPreferences()
    }

    private fun loadPreferences() {
        // TODO: Show loading indicator once plugin layers are in place
        viewLifecycleOwner.lifecycleScope.launch {
            val loader = AppServiceRegistry.get<MapLayerLoader>()
            val definitions = onDefault {
                loader.getDefinitions().values.filter { it.isConfigurable }
            }
            val manager = MapLayerPreferenceManager(mapId, definitions, alwaysEnabledLayerIds)
            val preferences = MapLayersBottomSheetFragment(
                manager,
                requireActivity() as MainActivity
            )
            replaceChildFragment(preferences, binding.preferencesFragment.id)
        }
    }
}

private data class MapLayerViews(
    val root: View,
    val title: Toolbar,
    val preferencesFragment: FragmentContainerView
)

@Composable
private fun MapLayerHost(
    views: MapLayerViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createMapLayerViews(context: Context): MapLayerViews {
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

    return MapLayerViews(root, title, preferences)
}
