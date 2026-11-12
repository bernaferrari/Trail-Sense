package com.kylecorry.trail_sense.tools.diagnostics.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.dialogs.DialogLabels
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticSeverity
import com.kylecorry.trail_sense.tools.tools.ui.items.DiagnosticItem

class DiagnosticAlerter {

    fun alert(
        item: DiagnosticItem,
        onAction: (ToolDiagnosticAction) -> Unit
    ) {
        val affectedTools = item.tools.sortedBy { it.name }.joinToString("\n") { "- ${it.name}" }

        val message = getAppService<AppStrings>().get(Res.string.diagnostic_message_template,
            getSeverityName(item.result.severity),
            item.result.description,
            affectedTools,
            item.result.resolution ?: getAppService<AppStrings>().get(Res.string.no_resolution)
        )

        Alerts.dialog(AppHost.current(),
            item.result.name,
            DependencyRegistry.get<MarkdownService>().toMarkdown(message),
            cancelText = if (item.result.action != null) DialogLabels.Cancel else null,
            okText = item.result.action?.name ?: DialogLabels.Ok
        ) { cancelled ->
            if (!cancelled) {
                item.result.action?.let(onAction)
            }
        }
    }


    private fun getSeverityName(status: ToolDiagnosticSeverity): String {
        return when (status) {
            ToolDiagnosticSeverity.Error -> getAppService<AppStrings>().get(Res.string.error)
            ToolDiagnosticSeverity.Warning -> getAppService<AppStrings>().get(Res.string.warning)
        }
    }

}
