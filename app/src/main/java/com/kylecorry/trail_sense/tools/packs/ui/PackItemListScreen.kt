@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.domain.PackService
import com.kylecorry.trail_sense.tools.packs.domain.sort.CategoryPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.PackedPercentPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.WeightPackItemSort
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryColorMapper
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryIconMapper
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import java.lang.Double.max
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PackItemListToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { PackItemListController(fragment) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {}
    }

    PackItemScreen(
        title = controller.title,
        items = controller.sortedItems,
        totalPercentPacked = controller.totalPercentPacked,
        totalPackedWeight = controller.totalPackedWeight,
        showWeightOverview = controller.showWeightOverview,
        onSort = controller::changeSort,
        onRename = { controller.pack?.let { current -> controller.renamePack(current) } },
        onExport = { controller.pack?.let { current -> controller.exportPack(current) } },
        onDeletePack = { controller.pack?.let { current -> controller.deletePack(current) } },
        onClearAmounts = controller::clearAmounts,
        onAddItem = controller::addItem,
        onItemClick = controller::editItem,
        onItemCheckedChange = controller::onItemCheckboxClicked,
        onAddAmount = controller::add,
        onSubtractAmount = controller::subtract,
        onEditItem = controller::editItem,
        onDeleteItem = controller::deleteItem,
        modifier = Modifier.fillMaxSize()
    )
}

private class PackItemListController(private val fragment: TrailSenseComposeDestinationFragment) {

    private val itemRepo by lazy { PackRepo.getInstance(fragment.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val packService = PackService()
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private var isInitialized = false

    private var items = emptyList<PackItem>()
    var sortedItems by mutableStateOf(emptyList<PackItem>())
        private set
    var pack: Pack? = null
        private set
    private var packId: Long = 0L
    var title by mutableStateOf("")
        private set
    var totalPercentPacked by mutableStateOf("")
        private set
    var totalPackedWeight by mutableStateOf("")
        private set
    var showWeightOverview by mutableStateOf(false)
        private set

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        packId = fragment.arguments?.getLong("pack_id") ?: 0L
        setupUI()
        fragment.inBackground {
            loadPack(packId)
        }
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
        fragment.observe(itemRepo.getItemsFromPack(packId)) {
            items = it
            updateItems()
        }
    }

    fun addItem() {
        fragment.findNavController().navigate(
            R.id.action_action_inventory_to_createItemFragment,
            Bundle().apply {
                putLong("pack_id", packId)
            }
        )
    }

    fun clearAmounts() {
        Alerts.dialog(
            fragment.requireContext(),
            fragment.getString(R.string.clear_amounts),
            fragment.getString(R.string.action_inventory_clear_confirm)
        ) { cancelled ->
            if (!cancelled) {
                fragment.inBackground {
                    withContext(Dispatchers.IO) {
                        itemRepo.clearPackedAmounts(packId)
                    }
                }
            }
        }
    }

    private fun onSortChange(newSort: String) {
        sortedItems = sorts[newSort]?.sort(items) ?: items
        prefs.packs.packSort = newSort
    }

    fun renamePack(pack: Pack) {
        Pickers.text(
            fragment.requireContext(),
            fragment.getString(R.string.rename),
            null,
            pack.name,
            hint = fragment.getString(R.string.name)
        ) {
            if (it != null) {
                fragment.inBackground {
                    val renamed = pack.copy(name = it)
                    withContext(Dispatchers.IO) {
                        itemRepo.addPack(renamed)
                    }
                    withContext(Dispatchers.Main) {
                        this@PackItemListController.pack = renamed
                        title = renamed.name
                    }
                }
            }
        }
    }

    fun exportPack(pack: Pack) {
        ExportPackingListCommand(fragment).execute(pack)
    }

    fun deletePack(pack: Pack) {
        Alerts.dialog(
            fragment.requireContext(),
            fragment.getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                fragment.inBackground {
                    withContext(Dispatchers.IO) {
                        itemRepo.deletePack(pack)
                    }
                    withContext(Dispatchers.Main) {
                        fragment.findNavController().popBackStack()
                    }
                }
            }
        }
    }

    fun onItemCheckboxClicked(item: PackItem) {
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

    fun deleteItem(item: PackItem) {
        fragment.inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.deleteItem(item)
            }
        }
    }

    fun editItem(item: PackItem) {
        val bundle = Bundle().apply {
            putLong("edit_item_id", item.id)
            putLong("pack_id", packId)
        }
        fragment.findNavController().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }

    fun add(item: PackItem) {
        Pickers.number(
            fragment.requireContext(),
            fragment.getString(R.string.add),
            null,
            null,
            allowNegative = false,
            hint = fragment.getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, it.toDouble())
            }
        }
    }

    fun subtract(item: PackItem) {
        Pickers.number(
            fragment.requireContext(),
            fragment.getString(R.string.subtract),
            null,
            null,
            allowNegative = false,
            hint = fragment.getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, -it.toDouble())
            }
        }
    }

    private fun addAmount(item: PackItem, amount: Double) {
        fragment.inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = max(0.0, item.amount + amount))
                )
            }
        }
    }

    private fun setAmount(item: PackItem, amount: Double) {
        fragment.inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = amount)
                )
            }
        }
    }

    fun changeSort() {
        val options = listOf("category", "percent_asc", "percent_desc", "weight_asc", "weight_desc")
        Pickers.item(
            fragment.requireContext(),
            fragment.getString(R.string.sort),
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
            "category" -> fragment.getString(R.string.category)
            "percent_asc" -> fragment.getString(R.string.pack_sort_percent_low_to_high)
            "percent_desc" -> fragment.getString(R.string.pack_sort_percent_high_to_low)
            "weight_asc" -> fragment.getString(R.string.pack_sort_weight_low_to_high)
            "weight_desc" -> fragment.getString(R.string.pack_sort_weight_high_to_low)
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
            fragment.getString(R.string.percent_packed, formatService.formatPercentage(packedPercent))
        sortedItems = sorts[prefs.packs.packSort]?.sort(items) ?: items
    }
}

@Composable
private fun PackItemScreen(
    title: String,
    items: List<PackItem>,
    totalPercentPacked: String,
    totalPackedWeight: String,
    showWeightOverview: Boolean,
    onSort: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDeletePack: () -> Unit,
    onClearAmounts: () -> Unit,
    onAddItem: () -> Unit,
    onItemClick: (PackItem) -> Unit,
    onItemCheckedChange: (PackItem) -> Unit,
    onAddAmount: (PackItem) -> Unit,
    onSubtractAmount: (PackItem) -> Unit,
    onEditItem: (PackItem) -> Unit,
    onDeleteItem: (PackItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            PackItemHeader(
                title = title,
                onSort = onSort,
                onRename = onRename,
                onExport = onExport,
                onDeletePack = onDeletePack,
                onClearAmounts = onClearAmounts
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                modifier = Modifier.testTag("add_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PackDetails(
                totalPercentPacked = totalPercentPacked,
                totalPackedWeight = totalPackedWeight,
                showWeightOverview = showWeightOverview
            )
            Box(modifier = Modifier.fillMaxSize()) {
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.inventory_empty_text),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("inventory_empty_text"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("inventory_list"),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            PackItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onCheckedChange = { onItemCheckedChange(item) },
                                onAddAmount = { onAddAmount(item) },
                                onSubtractAmount = { onSubtractAmount(item) },
                                onEditItem = { onEditItem(item) },
                                onDeleteItem = { onDeleteItem(item) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackItemHeader(
    title: String,
    onSort: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDeletePack: () -> Unit,
    onClearAmounts: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

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
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.testTag("inventory_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort)) }, onClick = {
                        isMenuOpen = false
                        onSort()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                        isMenuOpen = false
                        onRename()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.export)) }, onClick = {
                        isMenuOpen = false
                        onExport()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                        isMenuOpen = false
                        onDeletePack()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.clear_amounts)) }, onClick = {
                        isMenuOpen = false
                        onClearAmounts()
                    })
                }
            }
        }
    }
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
            tag = "total_percent_packed",
            modifier = Modifier.weight(1f)
        )
        if (showWeightOverview) {
            PackDetailStat(
                icon = R.drawable.ic_weight,
                text = totalPackedWeight,
                tag = "total_packed_weight",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PackDetailStat(
    icon: Int,
    text: String,
    tag: String,
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
        Text(
            text = text,
            modifier = Modifier
                .padding(start = 8.dp)
                .testTag(tag),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PackItemRow(
    item: PackItem,
    onClick: () -> Unit,
    onCheckedChange: () -> Unit,
    onAddAmount: () -> Unit,
    onSubtractAmount: () -> Unit,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val context = LocalContext.current
    val categoryTextMapper = remember(context) { ItemCategoryStringMapper(context) }
    val iconMapper = remember { ItemCategoryIconMapper() }
    val colorMapper = remember { ItemCategoryColorMapper() }
    val formatService = remember(context) { FormatService.getInstance(context) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val count = formatItemCount(item)
    val packedWeight = item.weight?.let {
        item.packedWeight?.let { weight ->
            formatService.formatWeight(weight, Units.getDecimalPlaces(weight.units), false)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, top = 10.dp, bottom = 10.dp)
            .testTag("inventory_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isFullyPacked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.testTag("item_checkbox")
        )
        Icon(
            painter = painterResource(iconMapper.getIcon(item.category)),
            contentDescription = null,
            tint = ComposeColor(colorMapper.map(item.category).color),
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.md)
        ) {
            Text(
                text = item.name,
                modifier = Modifier.testTag("item_title"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryTextMapper.getString(item.category),
                    modifier = Modifier.testTag("item_category"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.isOptional) {
                    Text(
                        text = stringResource(R.string.packing_list_optional),
                        modifier = Modifier.testTag("item_optional"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                Text(
                    text = count,
                    modifier = Modifier.testTag("item_count"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (packedWeight != null) {
                    Text(
                        text = packedWeight,
                        modifier = Modifier.testTag("item_weight"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.testTag("item_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                DropdownMenuItem(text = { Text(stringResource(R.string.add)) }, onClick = {
                    isMenuOpen = false
                    onAddAmount()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.subtract)) }, onClick = {
                    isMenuOpen = false
                    onSubtractAmount()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = {
                    isMenuOpen = false
                    onEditItem()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                    isMenuOpen = false
                    onDeleteItem()
                })
            }
        }
    }
}

private fun formatItemCount(item: PackItem): String {
    val currentAmount = DecimalFormatter.format(item.amount, 4, false)
    return if (item.desiredAmount != 0.0) {
        "$currentAmount / ${DecimalFormatter.format(item.desiredAmount, 4, false)}"
    } else {
        currentAmount
    }
}

@Preview(showBackground = true)
@Composable
private fun PackItemScreenPreview() {
    TrailSenseTheme {
        PackItemScreen(
            title = "Weekend Kit",
            items = emptyList(),
            totalPercentPacked = "50% packed",
            totalPackedWeight = "8 lb",
            showWeightOverview = true,
            onSort = {},
            onRename = {},
            onExport = {},
            onDeletePack = {},
            onClearAmounts = {},
            onAddItem = {},
            onItemClick = {},
            onItemCheckedChange = {},
            onAddAmount = {},
            onSubtractAmount = {},
            onEditItem = {},
            onDeleteItem = {}
        )
    }
}
