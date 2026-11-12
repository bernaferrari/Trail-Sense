package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context

class AndroidToolCatalog(private val context: Context) : ToolCatalog {
    override fun isToolAvailable(toolId: Long): Boolean = Tools.isToolAvailable(context, toolId)

    override fun getService(serviceId: String): ToolService? = Tools.getService(context, serviceId)
}
