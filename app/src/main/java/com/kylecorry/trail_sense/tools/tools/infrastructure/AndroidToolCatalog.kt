package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context

class AndroidToolCatalog(private val context: Context) : ToolCatalog {
    override fun getTools(availableOnly: Boolean): List<Tool> = Tools.getTools(context, availableOnly)

    override fun getTool(toolId: Long): Tool? = Tools.getTool(context, toolId)

    override fun isToolAvailable(toolId: Long): Boolean = Tools.isToolAvailable(context, toolId)

    override fun getService(serviceId: String): ToolService? = Tools.getService(context, serviceId)
}
