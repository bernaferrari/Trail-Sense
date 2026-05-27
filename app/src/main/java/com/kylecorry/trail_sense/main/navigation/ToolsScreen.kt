package com.kylecorry.trail_sense.main.navigation

import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun ToolsScreen(
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTools = remember { Tools.getTools(context) }
    var filter by remember { mutableStateOf("") }

    val filteredTools = remember(allTools, filter) {
        if (filter.isBlank()) allTools
        else allTools.filter {
            it.name.contains(filter, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.lg)
    ) {
        AndroidView(
            factory = { ctx ->
                SearchView(ctx).apply {
                    isIconified = false
                    queryHint = context.getString(R.string.search)
                    setOnQueryTextListener(object : OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false
                        override fun onQueryTextChange(newText: String?): Boolean {
                            filter = newText ?: ""
                            return true
                        }
                    })
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.sm)
        )

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
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolGridItem(
    tool: Tool,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(onClick = onClick),
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
