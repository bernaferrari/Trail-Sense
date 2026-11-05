@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

import androidx.compose.foundation.layout.Spacer
import com.kylecorry.trail_sense.shared.views.PackEmptyIllustration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.Alignment
import androidx.compose.material3.toShape
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesMiniBar
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesJoinNext
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryIconMapper
import com.kylecorry.trail_sense.shared.icons.trail.*

import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.observe
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.domain.PackService
import com.kylecorry.trail_sense.tools.packs.domain.sort.CategoryPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.PackedPercentPackItemSort
import com.kylecorry.trail_sense.tools.packs.domain.sort.WeightPackItemSort
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import java.lang.Double.max
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PackItemListToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PackItemListController(host) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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
        packedFraction = controller.packedFraction,
        packedCount = controller.packedCount,
        loaded = controller.loaded,
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
        modifier = Modifier.fillMaxSize(),
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class PackItemListController(private val host: TrailSenseDestinationHost) {

    private val itemRepo by lazy { PackRepo.getInstance(host.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val packService = PackService()
    private val prefs by lazy { UserPreferences(host.requireContext()) }
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
    var packedFraction by mutableFloatStateOf(0f)
        private set
    var packedCount by mutableStateOf(0 to 0)
        private set
    var loaded by mutableStateOf(false)
        private set

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        packId = host.destinationArguments?.getLong("pack_id") ?: 0L
        setupUI()
        host.inBackground {
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
        itemRepo.getItemsFromPack(packId).observe(host.lifecycleOwner) {
            items = it
            updateItems()
            loaded = true
        }
    }

    fun addItem() {
        host.findNavigator().navigate(
            R.id.action_action_inventory_to_createItemFragment,
            Bundle().apply {
                putLong("pack_id", packId)
            }
        )
    }

    fun clearAmounts() {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.clear_amounts),
            host.getString(R.string.action_inventory_clear_confirm)
        ) { cancelled ->
            if (!cancelled) {
                host.inBackground {
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
            host.requireContext(),
            host.getString(R.string.rename),
            null,
            pack.name,
            hint = host.getString(R.string.name)
        ) {
            if (it != null) {
                host.inBackground {
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
        ExportPackingListCommand(host).execute(pack)
    }

    fun deletePack(pack: Pack) {
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete_pack),
            pack.name
        ) { cancelled ->
            if (!cancelled) {
                host.inBackground {
                    withContext(Dispatchers.IO) {
                        itemRepo.deletePack(pack)
                    }
                    withContext(Dispatchers.Main) {
                        host.findNavigator().popBackStack()
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
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete_packing_list_item),
            item.name
        ) { cancelled ->
            if (!cancelled) {
                host.inBackground {
                    withContext(Dispatchers.IO) {
                        itemRepo.deleteItem(item)
                    }
                }
            }
        }
    }

    fun editItem(item: PackItem) {
        val bundle = Bundle().apply {
            putLong("edit_item_id", item.id)
            putLong("pack_id", packId)
        }
        host.findNavigator().navigate(R.id.action_action_inventory_to_createItemFragment, bundle)
    }

    fun add(item: PackItem) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.add),
            null,
            null,
            allowNegative = false,
            hint = host.getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, it.toDouble())
            }
        }
    }

    fun subtract(item: PackItem) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.subtract),
            null,
            null,
            allowNegative = false,
            hint = host.getString(R.string.dialog_item_amount)
        ) {
            if (it != null) {
                addAmount(item, -it.toDouble())
            }
        }
    }

    private fun addAmount(item: PackItem, amount: Double) {
        host.inBackground {
            withContext(Dispatchers.IO) {
                itemRepo.addItem(
                    item.copy(amount = max(0.0, item.amount + amount))
                )
            }
        }
    }

    private fun setAmount(item: PackItem, amount: Double) {
        host.inBackground {
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
            host.requireContext(),
            host.getString(R.string.sort),
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
            "category" -> host.getString(R.string.category)
            "percent_asc" -> host.getString(R.string.pack_sort_percent_low_to_high)
            "percent_desc" -> host.getString(R.string.pack_sort_percent_high_to_low)
            "weight_asc" -> host.getString(R.string.pack_sort_weight_low_to_high)
            "weight_desc" -> host.getString(R.string.pack_sort_weight_high_to_low)
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
            host.getString(R.string.percent_packed, formatService.formatPercentage(packedPercent))
        packedFraction = (packedPercent / 100f).coerceIn(0f, 1f)
        packedCount = items.count { it.isFullyPacked } to items.size
        sortedItems = sorts[prefs.packs.packSort]?.sort(items) ?: items
    }
}

/**
 * One packing list. The hero says how packed you are (percent, a progress line, how many items, the packed weight);
 * below it the items in their sorted order, consecutive items of a category as one titled group. Each row is a
 * checkbox (pack it all or unpack), the name, and a calm `count · weight · optional` line; tapping the row edits it
 * and its overflow adds or subtracts an amount. Sort sits in the top bar; the list's own actions in its overflow.
 */
@Composable
private fun PackItemScreen(
    title: String,
    items: List<PackItem>,
    totalPercentPacked: String,
    totalPackedWeight: String,
    showWeightOverview: Boolean,
    packedFraction: Float,
    packedCount: Pair<Int, Int>,
    loaded: Boolean,
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
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val categoryNames = remember(context) { ItemCategoryStringMapper(context) }

    // Group consecutive items by category, preserving the sorted order.
    val groups = remember(items) {
        val result = mutableListOf<Pair<ItemCategory, MutableList<PackItem>>>()
        items.forEach { item ->
            val last = result.lastOrNull()
            if (last != null && last.first == item.category) {
                last.second.add(item)
            } else {
                result.add(item.category to mutableListOf(item))
            }
        }
        result
    }

    TrailSensePage(
        title = title,
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("inventory_list_title"),
        actions = {
            IconButton(onClick = onSort, modifier = Modifier.testTag("inventory_sort_btn")) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.sort))
            }
            PacksOverflowMenu(
                contentDescription = stringResource(R.string.packs_more_options),
                modifier = Modifier.testTag("inventory_menu_btn"),
                actions = listOf(
                    PacksMenuAction(stringResource(R.string.rename), onRename),
                    PacksMenuAction(stringResource(R.string.clear_amounts), onClearAmounts),
                    PacksMenuAction(stringResource(R.string.export), onExport),
                    PacksMenuAction(stringResource(R.string.delete), onDeletePack)
                )
            )
        },
        floatingActionButton = {
            if (loaded && items.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = onAddItem,
                modifier = Modifier.testTag("add_btn"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) }
            )
        }
    ) {
        if (!loaded) return@TrailSensePage

        if (items.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.ToolPack,
                    illustration = { PackEmptyIllustration() },
                    title = stringResource(R.string.inventory_empty_text),
                    description = stringResource(R.string.packs_items_empty_description),
                    actionLabel = stringResource(R.string.create_item_title),
                    actionIcon = TrailIcons.Add,
                    onAction = onAddItem,
                    actionModifier = Modifier.testTag("add_btn"),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("inventory_empty_text")
                )
            }
            return@TrailSensePage
        }

        item(key = "pack_hero") {
            PackHeroCard(
                totalPercentPacked = totalPercentPacked,
                totalPackedWeight = totalPackedWeight,
                showWeightOverview = showWeightOverview,
                packedFraction = packedFraction,
                packedCount = packedCount
            )
        }

        groups.forEachIndexed { index, (category, categoryItems) ->
            item(key = "category_header_${index}_${category.id}") {
                CategoryHeader(
                    category = category,
                    name = categoryNames.getString(category),
                    packed = categoryItems.count { it.isFullyPacked },
                    total = categoryItems.size,
                    modifier = Modifier.placesJoinNext(true)
                )
            }
            trailSenseSection(
                key = "category_${index}_${category.id}"
            ) {
                categoryItems.forEach { item ->
                    row { position ->
                        PackItemRow(
                            item = item,
                            position = position,
                            onClick = { onItemClick(item) },
                            onCheckedChange = { onItemCheckedChange(item) },
                            onAddAmount = { onAddAmount(item) },
                            onSubtractAmount = { onSubtractAmount(item) },
                            onEditItem = { onEditItem(item) },
                            onDeleteItem = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

/** A category's header: its icon on a small cookie, its name, and how many of its items are packed. */
@Composable
private fun CategoryHeader(
    category: ItemCategory,
    name: String,
    packed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val iconMapper = remember { ItemCategoryIconMapper() }
    val status = trailSenseStatusColors()
    val done = packed == total
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TrailSenseSpacing.sm, end = TrailSenseSpacing.lg, top = TrailSenseSpacing.md, bottom = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlacesBadge(
            containerColor = if (done) status.goodContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = if (done) status.onGoodContainer else MaterialTheme.colorScheme.primary,
            icon = iconMapper.getIconVector(category),
            shape = MaterialShapes.Cookie6Sided.toShape(),
            size = 40.dp,
            iconScale = 0.55f
        )
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$packed / $total",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = if (done) status.good else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * How packed the list is, as the Spotlight: the percentage large beside a cookie that turns green with a check when
 * everything's in, a thick wavy bar and the item count, and the packed weight as one quiet line.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PackHeroCard(
    totalPercentPacked: String,
    totalPackedWeight: String,
    showWeightOverview: Boolean,
    packedFraction: Float,
    packedCount: Pair<Int, Int>
) {
    val progress by animateFloatAsState(
        targetValue = packedFraction,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "packedProgress"
    )
    val done = packedCount.second > 0 && packedCount.first >= packedCount.second
    val colors = trailSenseToneColors(if (done) TrailSenseTone.Good else TrailSenseTone.Primary)
    val content = colors.content
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TrailSenseShapeBadge(
                    icon = if (done) TrailIcons.Check else TrailIcons.ToolPack,
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    size = 64.dp,
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = totalPercentPacked,
                        modifier = Modifier.testTag("total_percent_packed"),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.packs_items_packed, packedCount.first, packedCount.second),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = content.copy(alpha = 0.82f)
                    )
                }
            }
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = colors.accent,
                trackColor = content.copy(alpha = 0.16f)
            )
            if (showWeightOverview) {
                // One quiet line, not a card inside the card.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.82f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = totalPackedWeight,
                        modifier = Modifier.testTag("total_packed_weight"),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PackItemRow(
    item: PackItem,
    position: TrailSenseGroupPosition,
    onClick: () -> Unit,
    onCheckedChange: () -> Unit,
    onAddAmount: () -> Unit,
    onSubtractAmount: () -> Unit,
    onEditItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val context = LocalContext.current
    val formatService = remember(context) { FormatService.getInstance(context) }
    val count = formatItemCount(item)
    val packedWeight = item.weight?.let {
        item.packedWeight?.let { weight ->
            formatService.formatWeight(weight, Units.getDecimalPlaces(weight.units), false)
        }
    }
    val titleColor by animateColorAsState(
        targetValue = if (item.isFullyPacked) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "packedTitle"
    )
    val supporting = MaterialTheme.typography.bodyMedium
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant

    PacksGroupRow(
        position = position,
        modifier = Modifier.testTag("inventory_item"),
        onClick = onClick,
        leading = {
            Checkbox(
                checked = item.isFullyPacked,
                onCheckedChange = { onCheckedChange() },
                modifier = Modifier.testTag("item_checkbox")
            )
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                Text(
                    text = count,
                    modifier = Modifier.testTag("item_count"),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isFullyPacked) trailSenseStatusColors().good else MaterialTheme.colorScheme.primary
                )
                if (item.desiredAmount != 0.0) {
                    PlacesMiniBar(
                        item.percentPacked / 100f,
                        color = if (item.isFullyPacked) trailSenseStatusColors().good else MaterialTheme.colorScheme.primary,
                        width = 48.dp
                    )
                }
            }
            PacksOverflowMenu(
                contentDescription = stringResource(R.string.packs_more_options),
                modifier = Modifier.testTag("item_menu"),
                actions = listOf(
                    PacksMenuAction(stringResource(R.string.add), onAddAmount),
                    PacksMenuAction(stringResource(R.string.subtract), onSubtractAmount),
                    PacksMenuAction(stringResource(R.string.edit), onEditItem),
                    PacksMenuAction(stringResource(R.string.delete), onDeleteItem)
                )
            )
        }
    ) {
        Text(
            text = item.name,
            modifier = Modifier.testTag("item_title"),
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
            if (packedWeight != null) {
                Text(
                    text = packedWeight,
                    modifier = Modifier.testTag("item_weight"),
                    style = supporting.copy(fontFeatureSettings = "tnum"),
                    color = supportingColor
                )
            }
            if (item.isOptional) {
                if (packedWeight != null) Text("·", style = supporting, color = supportingColor)
                Text(
                    text = stringResource(R.string.packing_list_optional),
                    modifier = Modifier.testTag("item_optional"),
                    style = supporting,
                    color = supportingColor,
                    maxLines = 1
                )
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
            title = "Weekend kit",
            items = listOf(
                PackItem(1, 1, "Water bottle", ItemCategory.Hydration, 1.0, 2.0),
                PackItem(2, 1, "Trail mix", ItemCategory.Food, 3.0, 3.0),
                PackItem(3, 1, "Rain jacket", ItemCategory.Clothing, 0.0, 1.0, isOptional = true)
            ),
            totalPercentPacked = "50% packed",
            totalPackedWeight = "8 lb",
            showWeightOverview = true,
            packedFraction = 0.5f,
            packedCount = 1 to 3,
            loaded = true,
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
