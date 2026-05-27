package com.kylecorry.trail_sense.tools.diagnostics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticResult
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

@Deprecated("Replaced by DiagnosticsScreen in Nav3. Remove once legacy navigation is fully removed.")
class DiagnosticsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        var results by remember { mutableStateOf<Map<String, List<ToolDiagnosticResult>>>(emptyMap()) }
        val resultsLock = remember { Any() }

        var diagnosticIdToTool by remember { mutableStateOf<Map<String, List<com.kylecorry.trail_sense.tools.tools.infrastructure.Tool>>>(emptyMap()) }

        val context = requireContext()
        remember {
            val tools = Tools.getTools(context)
            val toolMap = mutableMapOf<String, List<com.kylecorry.trail_sense.tools.tools.infrastructure.Tool>>()
            tools.forEach { tool ->
                tool.diagnostics.forEach { diagnostic ->
                    toolMap[diagnostic.id] = toolMap.getOrDefault(diagnostic.id, listOf()) + tool
                }
            }
            diagnosticIdToTool = toolMap

            val toolDiagnostics = tools.flatMap { it.diagnostics }.distinctBy { it.id }
            toolDiagnostics.forEach { diagnostic ->
                observeFlow(diagnostic.scanner.fullScan(context), BackgroundMinimumState.Resumed) { scanResults ->
                    synchronized(resultsLock) {
                        results = results + (diagnostic.id to scanResults)
                    }
                }
            }
            true
        }

        val items = remember(results, diagnosticIdToTool) {
            synchronized(resultsLock) {
                results.flatMap { (id, list) ->
                    list.map { DiagnosticItem(it, diagnosticIdToTool[id] ?: listOf()) }
                }.toSet().sortedBy { it.result.severity.ordinal }
            }
        }

        DiagnosticsList(
            items = items,
            onItemClick = { item ->
                DiagnosticAlerter(this).alert(item)
            }
        )
    }
}
