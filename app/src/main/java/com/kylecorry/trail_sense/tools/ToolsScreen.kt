package com.kylecorry.trail_sense.tools.tools.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSearch
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun ToolsScreen(
    onToolClick: (Tool) -> Unit,
    onToolLongClick: (Tool) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchFocusRequestChanged: (((() -> Boolean)?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTools = remember { Tools.getTools(context) }
    val toolSearch = remember { ToolSearch(context) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var filter by remember { mutableStateOf("") }

    DisposableEffect(onSearchFocusRequestChanged, keyboard) {
        onSearchFocusRequestChanged?.invoke {
            searchFocusRequester.requestFocus()
            keyboard?.show()
            true
        }
        onDispose {
            onSearchFocusRequestChanged?.invoke(null)
        }
    }

    val filteredTools = remember(allTools, filter, toolSearch) {
        if (filter.isBlank()) allTools
        else toolSearch.search(filter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.lg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        Spacer(Modifier.height(TrailSenseSpacing.sm))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = TrailSenseSpacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = filteredTools,
                key = { "tool_${it.id}" }
            ) { tool ->
                ToolGridItem(
                    tool = tool,
                    onClick = { onToolClick(tool) },
                    onLongClick = { onToolLongClick(tool) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolGridItem(
    tool: Tool,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(tool.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
