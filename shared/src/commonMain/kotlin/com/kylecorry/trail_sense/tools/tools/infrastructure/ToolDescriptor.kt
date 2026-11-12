package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import kotlin.reflect.KClass

/**
 * What every platform knows about a tool: how it is listed, where it opens and its guide. The Android tool adds its
 * widgets, tiles, services and other system hooks.
 */
interface ToolDescriptor : Identifiable {
    val name: String
    val icon: ImageVector
    val route: Route
    val category: ToolCategory
    val description: String?
    val guideId: RawText?
    val isExperimental: Boolean
    val settingsRoute: Route?

    /** Other screens that belong to this tool (its editors and details), so its tab stays lit on them. */
    val additionalRoutes: List<KClass<out Route>>

    /** Checks that tell the user why this tool may not work well on their device. */
    val diagnostics: List<ToolDiagnostic>

    fun isOpen(current: Route?): Boolean {
        current ?: return false
        return route::class == current::class || additionalRoutes.any { it.isInstance(current) }
    }
}
