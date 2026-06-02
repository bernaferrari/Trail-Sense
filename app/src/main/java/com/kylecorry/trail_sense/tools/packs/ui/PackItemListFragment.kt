package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.domain.PackService
import com.kylecorry.trail_sense.tools.packs.domain.sort.CategoryPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.PackedPercentPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.WeightPackItemSort
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackItemAction
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackItemListItemMapper
import java.lang.Double.max
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackItemListFragment : AndromedaFragment() {

    private val itemRepo by lazy { PackRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val packService = PackService()
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val listMapper by lazy { PackItemListItemMapper(requireContext(), this::handleAction) }

    private var items = emptyList<PackItem>()
    private var pack: Pack? = null
    private var packId: Long = 0L
    private var title by mutableStateOf("")
    private var totalPercentPacked by mutableStateOf("")
    private var totalPackedWeight by mutableStateOf("")
    private var showWeightOverview by mutableStateOf(false)
    private var packItemViews: PackItemViews? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packId = arguments?.getLong("pack_id") ?: 0L
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PackItemScreen(
                        title = title,
                        totalPercentPacked = totalPercentPacked,
                        totalPackedWeight = totalPackedWeight,
                        showWeightOverview = showWeightOverview,
                        onMenuClick = ::showMenu,
                        onAddItem = ::addItem,
                        onViewsReady = ::onViewsReady,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        inBackground {
            loadPack(packId)
        }
    }

    override fun onDestroyView() {
        packItemViews = null
        super.onDestroyView()
    }

    private fun onViewsReady(views: PackItemViews) {
        if (packItemViews != null) {
            return
        }

        packItemViews = views
        views.inventoryList.emptyView = views.emptyText
        updateItems()
    }

    private suspend fun loadPack(packId: Long) {
        val loaded = withContext(Dispatchers.IO) {
            itemRepo.getPack(packId)
        }
        withContext(Dispatchers.Main) {
            pack = loaded
            title = loaded?.name.orEmpty()
        }
    }

    private fun setupUI() {
        observe(itemRepo.getItemsFromPack(packId)) {
            items = it
            updateItems()
        }
    }

    private fun addItem() {
        findNavController().navigate(
            R.id.action_action_inventory_to_createItemFragment,
            Bundle().apply {
                putLong("pack_id", packId)
            }
        )
    }

    private fun showMenu(anchor: View) {
        Pickers.menu(
            anchor,
            listOf(
                getString(R.string.sort),
                getString(R.string.rename),
                getString(R.string.export),
                getString(R.string.delete),
                getString(R.string.clear_amounts)
            )
        ) {
            when (it) {
                0 -> changeSort()
                1 -> pack?.let { current -> renamePack(current) }
                2 -> pack?.let { current -> exportPack(current) }
                3 -> pack?.let { current -> deletePack(current) }
                4 -> pack?.let {
                    Alerts.dialog(
                        requireContext(),
                        getString(R.string.clear_amounts),
                        getString(R.string.action_inventory_clear_confirm)
                    ) { cancelled ->
                        if (!cancelled) {
                            inBackground {
                                withContext(Dispatchers.IO) {
                                    itemRepo.clearPackedAmounts(packId)
                                }
                            }
                        }
                    }
                }
            }
            true
        }
    }

    private fun onSortChange(newSort: String) {
        packItemViews?.inventoryList?.setItems(sorts[newSort]?.sort(items) ?: items, listMapper)
        prefs.packs.packSort = newSort
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
                    val renamed = pack.copy(name = it)
                    withContext(Dispatchers.IO) {
                        itemRepo.addPack(renamed)
                    }
                    withContext(Dispatchers.Main) {
                        this@PackItemListFragment.pack = renamed
                        title = renamed.name
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
                        itemRepo.deletePack(pack)
                    }
                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun onItemCheckboxClicked(item: PackItem) {
        if (!item.isFullyPacked) {
            if (item.desiredAmount == 0.0) {
                setAmount(item, 1.0)
            } else {
                setAmount(item, item.desiredAmount)
            }
        } else {
            setAmount(item, 0.0)
        }
    }

    private fun deleteItem(item: PackItem) {
        inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.deleteItem(item)
            }
        }
    }

    private fun editItem(item: PackItem) {
        val bundle = Bundle().apply {
            putLong("edit_item_id", item.id)
            putLong("pack_id", packId)
        }
        findNavController().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }

    private fun handleAction(item: PackItem, action: PackItemAction) {
        when (action) {
            PackItemAction.Check -> onItemCheckboxClicked(item)
            PackItemAction.Add -> add(item)
            PackItemAction.Subtract -> subtract(item)
            PackItemAction.Edit -> editItem(item)
            PackItemAction.Delete -> deleteItem(item)
        }
    }

    private fun add(item: PackItem) {
        Pickers.number(
            requireContext(),
            getString(R.string.add),
            null,
            null,
            allowNegative = false,
            hint = getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, it.toDouble())
            }
        }
    }

    private fun subtract(item: PackItem) {
        Pickers.number(
            requireContext(),
            getString(R.string.subtract),
            null,
            null,
            allowNegative = false,
            hint = getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, -it.toDouble())
            }
        }
    }

    private fun addAmount(item: PackItem, amount: Double) {
        inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = max(0.0, item.amount + amount))
                )
            }
        }
    }

    private fun setAmount(item: PackItem, amount: Double) {
        inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = amount)
                )
            }
        }
    }

    private fun changeSort() {
        val options = listOf("category", "percent_asc", "percent_desc", "weight_asc", "weight_desc")
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            options.map { getSortTitle(it) },
            options.indexOf(prefs.packs.packSort)
        ) {
            if (it != null) {
                onSortChange(options[it])
            }
        }
    }

    private val sorts = mapOf(
        "category" to CategoryPackItemSort(),
        "percent_asc" to PackedPercentPackItemSort(true),
        "percent_desc" to PackedPercentPackItemSort(false),
        "weight_asc" to WeightPackItemSort(true),
        "weight_desc" to WeightPackItemSort(false),
    )

    private fun getSortTitle(sort: String): String {
        return when (sort) {
            "category" -> getString(R.string.category)
            "percent_asc" -> getString(R.string.pack_sort_percent_low_to_high)
            "percent_desc" -> getString(R.string.pack_sort_percent_high_to_low)
            "weight_asc" -> getString(R.string.pack_sort_weight_low_to_high)
            "weight_desc" -> getString(R.string.pack_sort_weight_high_to_low)
            else -> ""
        }
    }

    private fun updateItems() {
        val totalWeight = packService.getPackWeight(items, prefs.weightUnits)
        val packedPercent = floor(packService.getPercentPacked(items))
        showWeightOverview = totalWeight != null
        totalPackedWeight = if (totalWeight != null) {
            formatService.formatWeight(
                totalWeight,
                Units.getDecimalPlaces(totalWeight.units),
                false
            )
        } else {
            ""
        }
        totalPercentPacked =
            getString(R.string.percent_packed, formatService.formatPercentage(packedPercent))
        packItemViews?.inventoryList?.setItems(
            sorts[prefs.packs.packSort]?.sort(items) ?: items,
            listMapper
        )
    }
}

private data class PackItemViews(
    val inventoryList: AndromedaListView,
    val emptyText: TextView
)

@Composable
private fun PackItemScreen(
    title: String,
    totalPercentPacked: String,
    totalPackedWeight: String,
    showWeightOverview: Boolean,
    onMenuClick: (View) -> Unit,
    onAddItem: () -> Unit,
    onViewsReady: (PackItemViews) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PackItemHeader(
                title = title,
                onMenuClick = onMenuClick
            )
            PackDetails(
                totalPercentPacked = totalPercentPacked,
                totalPackedWeight = totalPackedWeight,
                showWeightOverview = showWeightOverview
            )
            PackItemListHost(
                onAddItem = onAddItem,
                onViewsReady = onViewsReady,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PackItemHeader(
    title: String,
    onMenuClick: (View) -> Unit
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
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventory_list_title"),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
            HeaderMenuButton(onMenuClick)
        }
    }
}

@Composable
private fun HeaderMenuButton(onMenuClick: (View) -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(
        modifier = Modifier.testTag("inventory_menu_btn"),
        factory = { context ->
            ImageButton(context).apply {
                id = R.id.inventory_menu_btn
                background = null
                setImageResource(R.drawable.ic_menu_dots)
            }
        },
        update = {
            it.setColorFilter(tint)
            it.setOnClickListener { view -> onMenuClick(view) }
        }
    )
}

@Composable
private fun PackDetails(
    totalPercentPacked: String,
    totalPackedWeight: String,
    showWeightOverview: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PackDetailStat(
            icon = R.drawable.ic_tool_pack,
            text = totalPercentPacked,
            id = R.id.total_percent_packed,
            modifier = Modifier.weight(1f)
        )
        if (showWeightOverview) {
            PackDetailStat(
                icon = R.drawable.ic_weight,
                text = totalPackedWeight,
                id = R.id.total_packed_weight,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PackDetailStat(
    icon: Int,
    text: String,
    id: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AndroidView(
            modifier = Modifier.padding(start = 8.dp),
            factory = { context ->
                TextView(context).apply {
                    this.id = id
                }
            },
            update = {
                it.text = text
            }
        )
    }
}

@Composable
private fun PackItemListHost(
    onAddItem: () -> Unit,
    onViewsReady: (PackItemViews) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("inventory_list"),
        factory = { context ->
            FrameLayout(context).apply {
                val margin = Resources.dp(context, 16f).toInt()

                val list = AndromedaListView(context, null).apply {
                    id = R.id.inventory_list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val empty = TextView(context).apply {
                    id = R.id.inventory_empty_text
                    text = context.getString(R.string.inventory_empty_text)
                    gravity = Gravity.CENTER
                }

                val addButton = FloatingActionButton(context).apply {
                    id = R.id.add_btn
                    setImageResource(R.drawable.ic_add)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onAddItem() }
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
                    PackItemViews(
                        inventoryList = list,
                        emptyText = empty
                    )
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PackItemScreenPreview() {
    TrailSenseTheme {
        PackItemScreen(
            title = "Weekend Kit",
            totalPercentPacked = "50% packed",
            totalPackedWeight = "8 lb",
            showWeightOverview = true,
            onMenuClick = {},
            onAddItem = {},
            onViewsReady = {}
        )
    }
}
