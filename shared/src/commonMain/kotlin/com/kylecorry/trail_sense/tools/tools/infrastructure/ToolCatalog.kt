package com.kylecorry.trail_sense.tools.tools.infrastructure

/**
 * Which tools this device and platform offer, and the background services they run.
 */
interface ToolCatalog {
    /** The tools, without the ones this device can't run unless [availableOnly] is false. */
    fun getTools(availableOnly: Boolean = true): List<ToolDescriptor>
    fun getTool(toolId: Long): ToolDescriptor?
    fun isToolAvailable(toolId: Long): Boolean
    fun getService(serviceId: String): ToolService?
}
