@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.observe
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PackListToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PackListController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {}
    }

    PackListScreen(
        packs = controller.packs,
        onImportPack = controller::importPack,
        onCreatePack = controller::createPack,
        onOpenPack = { controller.openPack(it.id) },
        onRenamePack = controller::renamePack,
        onCopyPack = controller::copyPack,
        onExportPack = controller::exportPack,
        onDeletePack = controller::deletePack,
        modifier = Modifier.fillMaxSize()
    )
}

private class PackListController(private val host: TrailSenseDestinationHost) {

    private val packRepo by lazy { PackRepo.getInstance(host.requireContext()) }
    private var isInitialized = false

    var packs by mutableStateOf(emptyList<Pack>())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackListScreen(
    packs: List<Pack>,
    onImportPack: () -> Unit,
    onCreatePack: () -> Unit,
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreateMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.packing_lists),
                        modifier = Modifier.testTag("pack_list_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn"),
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                DropdownMenu(
                    expanded = isCreateMenuOpen,
                    onDismissRequest = { isCreateMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_packing_list)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onImportPack()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_packing_list)) },
                        onClick = {
                            isCreateMenuOpen = false
                            onCreatePack()
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (packs.isEmpty()) {
                TrailSenseEmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.no_packing_lists),
                    description = stringResource(R.string.new_packing_list),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("empty_text")
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pack_list"),
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.lg,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                ) {
                    items(packs, key = { it.id }) { pack ->
                        PackListItem(
                            pack = pack,
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
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    TrailSenseCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pack_list_item"),
        onClick = { onOpenPack(pack) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tonal circular icon chip
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tool_pack),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Text(
                text = pack.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .testTag("pack_list_item_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.testTag("pack_list_item_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = {
                            isMenuOpen = false
                            onRenamePack(pack)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(androidx.preference.R.string.copy)) },
                        onClick = {
                            isMenuOpen = false
                            onCopyPack(pack)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export)) },
                        onClick = {
                            isMenuOpen = false
                            onExportPack(pack)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            isMenuOpen = false
                            onDeletePack(pack)
                        }
                    )
                }
            }
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
