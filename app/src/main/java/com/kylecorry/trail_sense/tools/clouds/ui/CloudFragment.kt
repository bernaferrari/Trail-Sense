package com.kylecorry.trail_sense.tools.clouds.ui

import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrNull
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import java.time.Duration
import java.time.Instant

class CloudFragment : AndromedaFragment() {

    private val mapper by lazy { CloudReadingListItemMapper(requireContext(), this::handleAction) }
    private val repo by lazy { CloudRepo.getInstance(requireContext()) }
    private val cloudDetailsService by lazy { CloudDetailsService(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    private var clouds = emptyList<Reading<CloudObservation>>()
    private var subtitle by mutableStateOf("")
    private var cloudViews: CloudViews? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    CloudScreen(
                        subtitle = subtitle,
                        onGuideClick = ::showGuide,
                        onViewsReady = ::onViewsReady,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subtitle = getString(
            R.string.last_duration, formatter.formatDuration(
                Duration.ofHours(48),
                short = true
            )
        )

        observe(repo.getAllLive()) {
            val since = Instant.now().minus(Duration.ofHours(48))
            clouds = it.sortedByDescending { cloud -> cloud.time }
                .filter { cloud -> cloud.time >= since }
            updateCloudList()
        }

        val shouldOpenScanner = requireArguments().getBoolean("open_scanner", false)
        if (shouldOpenScanner) {
            addFromCamera()
        }
    }

    override fun onDestroyView() {
        cloudViews = null
        super.onDestroyView()
    }

    private fun showGuide() {
        UserGuideUtils.showGuide(this, R.raw.guide_tool_clouds)
    }

    private fun onViewsReady(views: CloudViews) {
        if (cloudViews != null) {
            return
        }

        cloudViews = views
        views.cloudList.emptyView = views.emptyText
        setupCreateMenu()
        updateCloudList()
    }

    private fun updateCloudList() {
        cloudViews?.cloudList?.setItems(clouds, mapper)
    }

    private fun setupCreateMenu() {
        val views = cloudViews ?: return
        views.addMenu.setOverlay(views.overlay)
        views.addMenu.fab = views.addButton
        views.addMenu.hideOnMenuOptionSelected = true
        views.addMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_cloud_camera -> addFromCamera()
                R.id.action_cloud_file -> addFromFile()
                R.id.action_cloud_manual -> addManual()
            }
            true
        }
    }

    private fun addFromCamera() {
        requestCamera {
            if (it) {
                inBackground {
                    val uri = CustomUiUtils.takePhoto(
                        this@CloudFragment,
                        Size(SoftmaxCloudClassifier.IMAGE_SIZE, SoftmaxCloudClassifier.IMAGE_SIZE)
                    )
                    uri?.let {
                        findNavController().navigate(
                            R.id.action_cloud_to_cloud_picker,
                            Bundle().apply {
                                putParcelable("image", uri)
                            }
                        )
                    }
                }
            } else {
                alertNoCameraPermission()
            }
        }
    }

    private fun addFromFile() {
        inBackground(BackgroundMinimumState.Created) {
            val uri =
                IntentUriPicker(this@CloudFragment, requireContext()).open(listOf("image/*"))
                    .getOrNull()
            val temp = uri?.let { onIO { files.copyToTemp(it) }?.toUri() }
            temp?.let {
                findNavController().navigate(
                    R.id.action_cloud_to_cloud_picker,
                    Bundle().apply {
                        putParcelable("image", it)
                    }
                )
            }
        }
    }

    private fun addManual() {
        findNavController().navigate(R.id.action_cloud_to_cloud_picker)
    }

    private fun handleAction(action: CloudReadingAction, reading: Reading<CloudObservation>) {
        when (action) {
            CloudReadingAction.Delete -> delete(reading)
        }
    }

    private fun delete(reading: Reading<CloudObservation>) {
        inBackground {
            val cancelled = onMain {
                CoroutineAlerts.dialog(
                    requireContext(),
                    getString(R.string.delete),
                    cloudDetailsService.getCloudName(reading.value.genus)
                )
            }
            if (!cancelled) {
                repo.delete(reading)
            }
        }
    }
}

private data class CloudViews(
    val cloudList: AndromedaListView,
    val emptyText: TextView,
    val overlay: View,
    val addMenu: FloatingActionButtonMenu,
    val addButton: FloatingActionButton
)

@Composable
private fun CloudScreen(
    subtitle: String,
    onGuideClick: () -> Unit,
    onViewsReady: (CloudViews) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CloudHeader(
                subtitle = subtitle,
                onGuideClick = onGuideClick
            )
            CloudListHost(
                onViewsReady = onViewsReady,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CloudHeader(
    subtitle: String,
    onGuideClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("cloud_list_title")
            ) {
                Text(
                    text = stringResource(R.string.clouds),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onGuideClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CloudListHost(
    onViewsReady: (CloudViews) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("cloud_list"),
        factory = { context ->
            FrameLayout(context).apply {
                val margin = Resources.dp(context, 16f).toInt()
                val fabBottomMargin = Resources.dp(context, 56f + 32f).toInt()

                val list = AndromedaListView(context, null).apply {
                    id = R.id.cloud_list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val empty = TextView(context).apply {
                    id = R.id.cloud_empty_text
                    text = context.getString(R.string.no_clouds)
                    gravity = Gravity.CENTER
                }

                val overlay = View(context).apply {
                    id = R.id.overlay_mask
                    alpha = 0.86f
                    setBackgroundColor(
                        Resources.getAndroidColorAttr(context, android.R.attr.colorBackground)
                    )
                    isClickable = true
                    visibility = View.GONE
                }

                val addButton = FloatingActionButton(context).apply {
                    id = R.id.add_btn
                    setImageResource(R.drawable.ic_add)
                    isClickable = true
                    isFocusable = true
                }

                val addMenu = FloatingActionButtonMenu(context, null).apply {
                    id = R.id.add_menu
                    setMenu(R.menu.add_cloud_menu)
                    visibility = View.GONE
                }

                addView(list)
                addView(
                    empty,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
                addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    addMenu,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM or Gravity.END
                    ).apply {
                        marginEnd = margin
                        bottomMargin = fabBottomMargin
                    }
                )
                addView(
                    addButton,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM or Gravity.END
                    ).apply {
                        marginEnd = margin
                        bottomMargin = margin
                    }
                )

                onViewsReady(
                    CloudViews(
                        cloudList = list,
                        emptyText = empty,
                        overlay = overlay,
                        addMenu = addMenu,
                        addButton = addButton
                    )
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CloudScreenPreview() {
    TrailSenseTheme {
        CloudScreen(
            subtitle = "Last 48h",
            onGuideClick = {},
            onViewsReady = {}
        )
    }
}
