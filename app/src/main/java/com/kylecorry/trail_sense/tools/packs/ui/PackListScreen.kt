@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

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

    PackListScreen(
        packs = controller.packs,
        loaded = controller.loaded,
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
            val bundle = Bundle().apply {
                putLong("pack_id", packId)
            }
            host.findNavController().navigate(R.id.action_pack_to_pack_items, bundle)
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

/**
 * Packing lists: every list as one grouped row (newest first) with its actions behind the overflow, and the add menu
 * (new or imported list) on the FAB. The empty state waits for the first load and offers to start a list.
 */
@Composable
private fun PackListScreen(
    packs: List<Pack>,
    loaded: Boolean,
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
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn"),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add)) }
                )
                DropdownMenu(
                    expanded = isCreateMenuOpen,
                    onDismissRequest = { isCreateMenuOpen = false },
                    shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_packing_list)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onCreatePack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_packing_list)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onImportPack()
                        }
                    )
                }
            }
        }
    ) {
        if (!loaded) return@TrailSensePage

        if (packs.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = ImageVector.vectorResource(R.drawable.ic_tool_pack),
                    title = stringResource(R.string.no_packing_lists),
                    description = stringResource(R.string.packs_empty_description),
                    action = {
                        FilledTonalButton(onClick = onCreatePack) {
                            Text(stringResource(R.string.new_packing_list))
                        }
                    },
                    modifier = Modifier
                        .fillParentMaxHeight(0.8f)
                        .testTag("empty_text")
                )
            }
            return@TrailSensePage
        }

        item(key = "packs") {
            TrailSenseRowGroup(modifier = Modifier.testTag("pack_list")) {
                packs.forEach { pack ->
                    row { position ->
                        PackListItem(
                            pack = pack,
                            position = position,
                            onOpenPack = onOpenPack,
                            onRenamePack = onRenamePack,
                            onCopyPack = onCopyPack,
                            onExportPack = onExportPack,
                            onDeletePack = onDeletePack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackListItem(
    pack: Pack,
    position: TrailSenseGroupPosition,
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit
) {
    PacksGroupRow(
        position = position,
        modifier = Modifier.testTag("pack_list_item"),
        onClick = { onOpenPack(pack) },
        leading = { PacksIconWell(rememberVectorPainter(TrailIcons.ToolPack)) },
        trailing = {
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
