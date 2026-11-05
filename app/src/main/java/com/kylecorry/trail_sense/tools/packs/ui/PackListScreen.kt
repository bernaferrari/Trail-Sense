@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.animation.core.animateFloatAsState
import com.kylecorry.trail_sense.shared.views.PackEmptyIllustration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesMiniBar
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
import com.kylecorry.trail_sense.tools.packs.domain.PackService
import kotlin.math.roundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.observe
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PackListToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PackListController(host) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {}
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) controller.refreshSummaries()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PackListScreen(
        packs = controller.packs,
        loaded = controller.loaded,
        summaries = controller.summaries,
        onImportPack = controller::importPack,
        onCreatePack = controller::createPack,
        onOpenPack = { controller.openPack(it.id) },
        onRenamePack = controller::renamePack,
        onCopyPack = controller::copyPack,
        onExportPack = controller::exportPack,
        onDeletePack = controller::deletePack,
        modifier = Modifier.fillMaxSize(),
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class PackListController(private val host: TrailSenseDestinationHost) {

    private val packRepo by lazy { PackRepo.getInstance(host.requireContext()) }
    private var isInitialized = false

    var packs by mutableStateOf(emptyList<Pack>())
        private set
    var loaded by mutableStateOf(false)
        private set
    var summaries by mutableStateOf(emptyMap<Long, PackSummary>())
        private set

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        loadPacks()
    }

    private fun loadPacks() {
        packRepo.getPacks().observe(host.lifecycleOwner) {
            packs = it.sortedWith(compareBy { pack -> -pack.id })
            loaded = true
            loadSummaries(packs)
        }
    }

    fun refreshSummaries() {
        if (loaded) loadSummaries(packs)
    }

    private fun loadSummaries(packs: List<Pack>) {
        host.inBackground {
            val service = PackService()
            val result = withContext(Dispatchers.IO) {
                packs.associate { pack ->
                    val items = packRepo.getItemsFromPackAsync(pack.id)
                    pack.id to PackSummary(
                        itemCount = items.size,
                        packedCount = items.count { it.isFullyPacked },
                        percentPacked = if (items.isEmpty()) 0f else service.getPercentPacked(items)
                    )
                }
            }
            summaries = result
        }
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
                    withContext(Dispatchers.IO) {
                        packRepo.addPack(pack.copy(name = it))
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
                        packRepo.deletePack(pack)
                    }
                }
            }
        }
    }

    fun openPack(packId: Long) {
        tryOrNothing {
            host.findNavigator().navigate(Route.PackItemList(packId))
        }
    }

    fun importPack() {
        ImportPackingListCommand(host).execute()
    }

    fun createPack() {
        Pickers.text(
            host.requireContext(),
            host.getString(R.string.new_packing_list),
            null,
            null,
            hint = host.getString(R.string.name)
        ) {
            if (it != null) {
                host.inBackground {
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

    fun copyPack(oldPack: Pack) {
        Pickers.text(
            host.requireContext(),
            host.getString(R.string.new_packing_list),
            null,
            null,
            hint = host.getString(R.string.name)
        ) {
            if (it != null) {
                host.inBackground {
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

}

/** How far along a packing list is, for its row and the Spotlight. */
internal data class PackSummary(val itemCount: Int, val packedCount: Int, val percentPacked: Float)

/**
 * Packing lists: every list as a row with its packed share as a value and a mini bar, newest first. Add (new or
 * imported list) stays on the floating button, the screen's only add action, including when there are no lists.
 */
@Composable
private fun PackListScreen(
    packs: List<Pack>,
    loaded: Boolean,
    summaries: Map<Long, PackSummary>,
    onImportPack: () -> Unit,
    onCreatePack: () -> Unit,
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var isCreateMenuOpen by remember { mutableStateOf(false) }

    TrailSensePage(
        title = stringResource(R.string.packing_lists),
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("pack_list_title"),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.sm,
            // Room for the FAB under the last list; an empty screen needs none, and the extra padding squeezed the
            // empty state until its button's label was cut off.
            bottom = if (loaded && packs.isEmpty()) TrailSenseSpacing.xxxl else 112.dp
        ),
        // No overflow menu: its one item, import, is in the add menu, and on the empty page beside the first list.
        floatingActionButton = {
            // No FAB while empty: the empty state's in-flow button starts the first list.
            if (loaded && packs.isNotEmpty()) Box {
                ExtendedFloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add)) }
                )
                DropdownMenu(
                    expanded = isCreateMenuOpen,
                    onDismissRequest = { isCreateMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_packing_list)) },
                        leadingIcon = { Icon(TrailIcons.Add, contentDescription = null) },
                        onClick = {
                            isCreateMenuOpen = false
                            onCreatePack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_packing_list)) },
                        leadingIcon = { Icon(TrailIcons.ImportExport, contentDescription = null) },
                        onClick = {
                            isCreateMenuOpen = false
                            onImportPack()
                        }
                    )
                }
            }
        }
    ) {
        // Am I packed? Each list's packed share answers it, newest first, in one grouped list; the FAB is the one
        // action (its menu also imports).
        if (!loaded) return@TrailSensePage

        if (packs.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.ToolPack,
                    illustration = { PackEmptyIllustration() },
                    title = stringResource(R.string.no_packing_lists),
                    description = stringResource(R.string.packs_empty_description),
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .testTag("empty_text"),
                    actionLabel = stringResource(R.string.new_packing_list),
                    actionIcon = Icons.Rounded.Add,
                    onAction = onCreatePack,
                    actionModifier = Modifier.testTag("add_btn"),
                    secondaryActionLabel = stringResource(R.string.import_packing_list),
                    onSecondaryAction = onImportPack
                )
            }
            return@TrailSensePage
        }

        placesGroup(
            key = "packs",
            items = packs,
            itemKey = { it.id }
        ) { pack, position, rowModifier ->
            PackListItem(
                pack = pack,
                summary = summaries[pack.id],
                position = position,
                modifier = if (position.isFirst) rowModifier.testTag("pack_list") else rowModifier,
                onOpenPack = onOpenPack,
                onRenamePack = onRenamePack,
                onCopyPack = onCopyPack,
                onExportPack = onExportPack,
                onDeletePack = onDeletePack
            )
        }
    }
}

@Composable
private fun PackListItem(
    pack: Pack,
    summary: PackSummary?,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    val percent = summary?.percentPacked ?: 0f
    val done = summary != null && summary.itemCount > 0 && percent >= 100f
    PacksGroupRow(
        position = position,
        modifier = modifier.testTag("pack_list_item"),
        onClick = { onOpenPack(pack) },
        leading = {
            // The family's 40 dp well; a packed list turns it to the good colour with a check.
            PlacesBadge(
                containerColor = if (done) status.goodContainer else scheme.primary.copy(alpha = 0.12f),
                contentColor = if (done) status.onGoodContainer else scheme.primary,
                icon = if (done) TrailIcons.Check else TrailIcons.ToolPack,
                shape = MaterialShapes.Cookie6Sided.toShape(),
                size = 40.dp,
                iconScale = 0.55f,
                modifier = Modifier.padding(start = TrailSenseSpacing.sm)
            )
        },
        trailing = {
            if (summary != null && summary.itemCount > 0) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                    Text(
                        stringResource(R.string.places_percent, percent.roundToInt()),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = if (done) status.good else scheme.onSurface
                    )
                    PlacesMiniBar(
                        percent / 100f,
                        color = if (done) status.good else scheme.primary
                    )
                }
            }
            PacksOverflowMenu(
                contentDescription = stringResource(R.string.packs_more_options),
                modifier = Modifier.testTag("pack_list_item_menu"),
                actions = listOf(
                    PacksMenuAction(stringResource(R.string.rename)) { onRenamePack(pack) },
                    PacksMenuAction(stringResource(androidx.preference.R.string.copy)) { onCopyPack(pack) },
                    PacksMenuAction(stringResource(R.string.export)) { onExportPack(pack) },
                    PacksMenuAction(stringResource(R.string.delete)) { onDeletePack(pack) }
                )
            )
        }
    ) {
        Text(
            text = pack.name,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pack_list_item_title"),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (summary != null) {
            Text(
                text = pluralStringResource(R.plurals.places_packs_items, summary.itemCount, summary.itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PackListScreenPreview() {
    TrailSenseTheme {
        PackListScreen(
            packs = listOf(
                Pack(1, "Weekend camping"),
                Pack(2, "Day hike")
            ),
            loaded = true,
            summaries = emptyMap(),
            onImportPack = {},
            onCreatePack = {},
            onOpenPack = {},
            onRenamePack = {},
            onCopyPack = {},
            onExportPack = {},
            onDeletePack = {}
        )
    }
}
