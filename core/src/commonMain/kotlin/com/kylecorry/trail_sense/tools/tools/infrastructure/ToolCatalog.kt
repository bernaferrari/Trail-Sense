package com.kylecorry.trail_sense.tools.tools.infrastructure

/**
 * Which tools this device and platform offer, and the background services they run.
 */
interface ToolCatalog {
    fun isToolAvailable(toolId: Long): Boolean
    fun getService(serviceId: String): ToolService?
}
