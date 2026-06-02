package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackAction
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackListItemMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackListFragment : AndromedaFragment() {

    private val packRepo by lazy { PackRepo.getInstance(requireContext()) }
    private val listMapper by lazy { PackListItemMapper(requireContext(), this::handlePackAction) }

    private var packs = emptyList<Pack>()
    private var packViews: PackListViews? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PackListScreen(
                        onViewsReady = ::onViewsReady,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPacks()
    }

    override fun onDestroyView() {
        packViews = null
        super.onDestroyView()
    }

    private fun onViewsReady(views: PackListViews) {
        if (packViews != null) {
            return
        }

        packViews = views
        views.packList.emptyView = views.emptyText
        bindCreateMenu()
        updatePackList()
    }

    private fun handlePackAction(pack: Pack, action: PackAction) {
        when (action) {
            PackAction.Rename -> renamePack(pack)
            PackAction.Copy -> copyPack(pack)
            PackAction.Delete -> deletePack(pack)
            PackAction.Open -> openPack(pack.id)
            PackAction.Export -> exportPack(pack)
        }
    }

    private fun loadPacks() {
        observe(packRepo.getPacks()) {
            packs = it.sortedWith(compareBy { -it.id })
            updatePackList()
        }
    }

    private fun renamePack(pack: Pack) {
        Pickers.text(
            requireContext(),
            getString(R.string.rename),
            null,
            pack.name,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    withContext(Dispatchers.IO) {
                        packRepo.addPack(pack.copy(name = it))
                    }
                }
            }
        }
    }

    private fun exportPack(pack: Pack) {
        ExportPackingListCommand(this).execute(pack)
    }

    private fun deletePack(pack: Pack) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    withContext(Dispatchers.IO) {
                        packRepo.deletePack(pack)
                    }
                }
            }
        }
    }

    private fun openPack(packId: Long) {
        tryOrNothing {
            val bundle = Bundle().apply {
                putLong("pack_id", packId)
            }
            findNavController().navigate(R.id.action_pack_to_pack_items, bundle)
        }
    }

    private fun createPack() {
        Pickers.text(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    val packId = withContext(Dispatchers.IO) {
                        packRepo.addPack(Pack(0, it))
                    }

                    withContext(Dispatchers.Main) {
                        openPack(packId)
                    }
                }
            }
        }
    }

    private fun copyPack(oldPack: Pack) {
        Pickers.text(
            requireContext(),
            getString(R.string.new_packing_list),
            null,
            null,
            hint = getString(R.string.name)
        ) {
            if (it != null) {
                inBackground {
                    val packId = withContext(Dispatchers.IO) {
                        packRepo.copyPack(oldPack, Pack(0, it))
                    }

                    withContext(Dispatchers.Main) {
                        openPack(packId)
                    }
                }
            }
        }
    }

    private fun bindCreateMenu() {
        val views = packViews ?: return
        views.createMenu.setOverlay(views.overlay)
        views.createMenu.fab = views.addButton
        views.createMenu.hideOnMenuOptionSelected = true
        views.createMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_import_packing_list -> {
                    setCreateMenuVisibility(false)
                    ImportPackingListCommand(this).execute()
                }

                R.id.action_create_packing_list -> {
                    setCreateMenuVisibility(false)
                    createPack()
                }
            }
            true
        }

        onBackPressed {
            when {
                isCreateMenuOpen() -> {
                    setCreateMenuVisibility(false)
                }

                else -> {
                    remove()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setCreateMenuVisibility(isShowing: Boolean) {
        val createMenu = packViews?.createMenu ?: return
        if (isShowing) {
            createMenu.show()
        } else {
            createMenu.hide()
        }
    }

    private fun isCreateMenuOpen(): Boolean {
        return packViews?.createMenu?.isVisible == true
    }

    private fun updatePackList() {
        packViews?.packList?.setItems(packs, listMapper)
    }

}

private data class PackListViews(
    val packList: AndromedaListView,
    val emptyText: TextView,
    val overlay: View,
    val createMenu: FloatingActionButtonMenu,
    val addButton: FloatingActionButton
)

@Composable
private fun PackListScreen(
    onViewsReady: (PackListViews) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PackListHeader()
            PackListHost(
                onViewsReady = onViewsReady,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PackListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.packing_lists),
                modifier = Modifier.testTag("pack_list_title"),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun PackListHost(
    onViewsReady: (PackListViews) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("pack_list"),
        factory = { context ->
            FrameLayout(context).apply {
                val margin = Resources.dp(context, 16f).toInt()
                val fabBottomMargin = Resources.dp(context, 56f + 32f).toInt()

                val list = AndromedaListView(context, null).apply {
                    id = R.id.pack_list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val empty = TextView(context).apply {
                    id = R.id.empty_text
                    text = context.getString(R.string.no_packing_lists)
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
                    contentDescription = context.getString(R.string.add)
                    setImageResource(R.drawable.ic_add)
                    isClickable = true
                    isFocusable = true
                }

                val createMenu = FloatingActionButtonMenu(context, null).apply {
                    id = R.id.create_menu
                    setMenu(R.menu.add_pack_menu)
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
                    createMenu,
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
                    PackListViews(
                        packList = list,
                        emptyText = empty,
                        overlay = overlay,
                        createMenu = createMenu,
                        addButton = addButton
                    )
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PackListScreenPreview() {
    TrailSenseTheme {
        PackListScreen(onViewsReady = {})
    }
}
