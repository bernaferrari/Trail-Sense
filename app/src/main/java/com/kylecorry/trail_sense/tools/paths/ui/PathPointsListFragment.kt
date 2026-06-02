package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class PathPointsListFragment : BottomSheetDialogFragment() {

    private var points: List<PathPoint> = emptyList()
    private var adapter: PathPointAdapter? = null
    private var pathPointViews: PathPointViews? = null
    private val binding: PathPointViews
        get() = pathPointViews ?: error("Path point views are not ready")
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    var onCreateBeaconListener: (point: PathPoint) -> Unit = {}
    var onDeletePointListener: (point: PathPoint) -> Unit = {}
    var onNavigateToPointListener: (point: PathPoint) -> Unit = {}
    var onViewPointListener: (point: PathPoint) -> Unit = {}

    fun setPoints(points: List<PathPoint>) {
        this.points = points
        adapter?.setData(points)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createPathPointViews(requireContext())
        pathPointViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PathPointHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        pathPointViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PathPointAdapter().also {
            binding.pathPointsList.layoutManager = LinearLayoutManager(requireContext())
            binding.pathPointsList.adapter = it
            binding.pathPointsList.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            it.setData(points)
        }
    }

    private fun drawWaypointListItem(itemView: WaypointListItemView, item: PathPoint) {
        val itemStrategy = WaypointListItem(
            requireContext(),
            formatService,
            {
                onCreateBeaconListener(it)
                dismiss()
            },
            { onDeletePointListener(it) },
            {
                onNavigateToPointListener(it)
                dismiss()
            },
            {
                onViewPointListener(it)
                dismiss()
            }
        )

        itemStrategy.display(itemView, item)
    }

    private inner class PathPointAdapter : RecyclerView.Adapter<PathPointViewHolder>() {
        private var data = emptyList<PathPoint>()

        fun setData(points: List<PathPoint>) {
            data = points
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PathPointViewHolder {
            return PathPointViewHolder(WaypointListItemView(parent.context))
        }

        override fun onBindViewHolder(holder: PathPointViewHolder, position: Int) {
            drawWaypointListItem(holder.view, data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    private class PathPointViewHolder(
        val view: WaypointListItemView
    ) : RecyclerView.ViewHolder(view)
}

private data class PathPointViews(
    val root: View,
    val pathPointsList: RecyclerView
)

@Composable
private fun PathPointHost(
    views: PathPointViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createPathPointViews(context: Context): PathPointViews {
    val root = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val list = RecyclerView(context).apply {
        id = R.id.path_points_list
    }
    root.addView(
        list,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    return PathPointViews(root, list)
}

@Preview(showBackground = true)
@Composable
private fun PathPointHostPreview() {
    TrailSenseTheme {
        PathPointHost(
            views = createPathPointViews(LocalContext.current)
        )
    }
}
