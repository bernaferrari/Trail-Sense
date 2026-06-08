package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackListFragment : TrailSenseAndromedaComposeFragment() {

    private val packRepo by lazy { PackRepo.getInstance(requireContext()) }

    private var packs by mutableStateOf(emptyList<Pack>())

    @Composable
    override fun FragmentContent() {
        PackListScreen(
            packs = packs,
            onImportPack = ::importPack,
            onCreatePack = ::createPack,
            onOpenPack = { openPack(it.id) },
            onRenamePack = ::renamePack,
            onCopyPack = ::copyPack,
            onExportPack = ::exportPack,
            onDeletePack = ::deletePack,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPacks()
    }

    private fun loadPacks() {
        observe(packRepo.getPacks()) {
            packs = it.sortedWith(compareBy { pack -> -pack.id })
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

    private fun importPack() {
        ImportPackingListCommand(this).execute()
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

}

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
        topBar = { PackListHeader() },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { isCreateMenuOpen = true },
                    modifier = Modifier.testTag("add_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                }
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
                Text(
                    text = stringResource(R.string.no_packing_lists),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("empty_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pack_list"),
                    contentPadding = PaddingValues(bottom = 96.dp)
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
                        HorizontalDivider()
                    }
                }
            }
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
private fun PackListItem(
    pack: Pack,
    onOpenPack: (Pack) -> Unit,
    onRenamePack: (Pack) -> Unit,
    onCopyPack: (Pack) -> Unit,
    onExportPack: (Pack) -> Unit,
    onDeletePack: (Pack) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPack(pack) }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
            .testTag("pack_list_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tool_pack),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = pack.name,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TrailSenseSpacing.md)
                .testTag("pack_list_item_title"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.testTag("pack_list_item_menu")
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
