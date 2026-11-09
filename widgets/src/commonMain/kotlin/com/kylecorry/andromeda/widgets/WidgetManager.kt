package com.kylecorry.andromeda.widgets

/** Refreshes a widget through host-provided platform integration. */
class WidgetManager(
    private val updateWidget: suspend () -> Unit,
    private val installedWidget: suspend () -> Boolean? = { null },
) {
    suspend fun update() = updateWidget()
    /** Returns null when the platform cannot report whether the widget is installed. */
    suspend fun isInstalled(): Boolean? = installedWidget()
}
