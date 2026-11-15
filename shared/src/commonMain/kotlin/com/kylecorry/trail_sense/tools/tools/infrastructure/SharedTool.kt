package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import kotlin.reflect.KClass

/** The platform-independent identity and presentation of a tool. */
data class SharedTool(
    override val id: Long,
    override val name: String,
    override val icon: ImageVector,
    override val route: Route,
    override val category: ToolCategory,
    override val description: String? = null,
    override val guideId: RawText? = null,
    override val isExperimental: Boolean = false,
    override val settingsRoute: Route? = null,
    override val additionalRoutes: List<KClass<out Route>> = emptyList(),
    override val diagnostics: List<ToolDiagnostic> = emptyList()
) : ToolDescriptor
