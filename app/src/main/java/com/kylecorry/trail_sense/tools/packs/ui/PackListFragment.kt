package com.kylecorry.trail_sense.tools.packs.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useLiveData
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.commands.ExportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.commands.ImportPackingListCommand
import com.kylecorry.trail_sense.tools.packs.ui.mappers.PackAction

/**
 * Modern expressive Compose replacement for the legacy pack list screen.
 * Uses design system components (TrailSenseListItem, spacing, animations) for beautiful preference-like list UI.
 */
@Composable
fun Fragment.PackListContent(modifier: Modifier = Modifier) {
    val repo = useService<PackRepo>()
    val nav = findNavController()
    val context = requireContext()

    val packsLive = remember { repo.getPacks() }
    val packs by useLiveData(packsLive, emptyList()) { list ->
        list.sortedWith(compareBy { -it.id })
    }

    var showCreateMenu by remember { mutableStateOf(false) }

    // Ensure menu hides on back (matches old onBackPressed behavior)
    BackHandler(enabled = showCreateMenu) {
        showCreateMenu = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.packing_lists),
                        modifier = Modifier.testTag("pack_list_title")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showCreateMenu = !showCreateMenu },
                    modifier = Modifier.testTag("add_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = { showCreateMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.new_packing_list)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_tool_pack), contentDescription = null) },
                        onClick = {
                            showCreateMenu = false
                            createPack(this@PackListContent, repo, nav)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_packing_list)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_file), contentDescription = null) },
                        onClick = {
                            showCreateMenu = false
                            ImportPackingListCommand(this@PackListContent).execute()
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
                    icon = Icons.Default.Add,
                    title = stringResource(R.string.no_packing_lists),
                    description = null,
                    modifier = Modifier.testTag("empty_text")
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = TrailSenseSpacing.lg,
                        vertical = TrailSenseSpacing.md
                    ),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    items(packs, key = { it.id }) { pack ->
                        PackRow(
                            pack = pack,
                            onAction = { action ->
                                handlePackAction(
                                    this@PackListContent,
                                    repo,
                                    nav,
                                    pack,
                                    action
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(
    pack: Pack,
    onAction: (PackAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TrailSenseListItem(
        title = pack.name,
        icon = painterResource(R.drawable.ic_tool_pack),
        modifier = modifier.testTag("pack_row_${pack.id}"),
        onClick = { onAction(PackAction.Open) },
        trailing = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.more)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = {
                            menuExpanded = false
                            onAction(PackAction.Rename)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(androidx.preference.R.string.copy)) },
                        onClick = {
                            menuExpanded = false
                            onAction(PackAction.Copy)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export)) },
                        onClick = {
                            menuExpanded = false
                            onAction(PackAction.Export)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuExpanded = false
                            onAction(PackAction.Delete)
                        }
                    )
                }
            }
        }
    )
}

private fun createPack(fragment: Fragment, repo: PackRepo, nav: androidx.navigation.NavController) {
    Pickers.text(
        fragment.requireContext(),
        fragment.getString(R.string.new_packing_list),
        null,
        null,
        hint = fragment.getString(R.string.name)
    ) { name ->
        if (name != null) {
            fragment.inBackground {
                val id = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repo.addPack(Pack(0, name))
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    openPack(fragment, nav, id)
                }
            }
        }
    }
}

private fun handlePackAction(
    fragment: Fragment,
    repo: PackRepo,
    nav: androidx.navigation.NavController,
    pack: Pack,
    action: PackAction
) {
    when (action) {
        PackAction.Rename -> renamePack(fragment, repo, pack)
        PackAction.Copy -> copyPack(fragment, repo, nav, pack)
        PackAction.Delete -> deletePack(fragment, repo, pack)
        PackAction.Open -> openPack(fragment, nav, pack.id)
        PackAction.Export -> ExportPackingListCommand(fragment as com.kylecorry.andromeda.fragments.AndromedaFragment).execute(pack)
    }
}

private fun renamePack(fragment: Fragment, repo: PackRepo, pack: Pack) {
    Pickers.text(
        fragment.requireContext(),
        fragment.getString(R.string.rename),
        null,
        pack.name,
        hint = fragment.getString(R.string.name)
    ) { newName ->
        if (newName != null) {
            fragment.inBackground {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repo.addPack(pack.copy(name = newName))
                }
            }
        }
    }
}

private fun copyPack(fragment: Fragment, repo: PackRepo, nav: androidx.navigation.NavController, oldPack: Pack) {
    Pickers.text(
        fragment.requireContext(),
        fragment.getString(R.string.new_packing_list),
        null,
        null,
        hint = fragment.getString(R.string.name)
    ) { name ->
        if (name != null) {
            fragment.inBackground {
                val newId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repo.copyPack(oldPack, Pack(0, name))
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    openPack(fragment, nav, newId)
                }
            }
        }
    }
}

private fun deletePack(fragment: Fragment, repo: PackRepo, pack: Pack) {
    Alerts.dialog(
        fragment.requireContext(),
        fragment.getString(R.string.delete_pack),
        pack.name
    ) { cancelled ->
        if (!cancelled) {
            fragment.inBackground {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repo.deletePack(pack)
                }
            }
        }
    }
}

private fun openPack(fragment: Fragment, nav: androidx.navigation.NavController, packId: Long) {
    tryOrNothing {
        val bundle = Bundle().apply { putLong("pack_id", packId) }
        nav.navigate(R.id.action_pack_to_pack_items, bundle)
    }
}

class PackListFragment : TrailSenseComposeFragment() {
    @Composable
    override fun FragmentContent() {
        PackListContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun PackListPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            TrailSenseEmptyState(
                icon = Icons.Default.Add,
                title = "No packing lists",
                modifier = Modifier.testTag("empty_text")
            )
        }
    }
}
