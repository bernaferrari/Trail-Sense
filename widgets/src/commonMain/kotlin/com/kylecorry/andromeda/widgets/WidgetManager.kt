package com.kylecorry.andromeda.widgets

/** Refreshes a widget through host-provided platform integration. */
public class WidgetManager(
    private val updateWidget: suspend () -> Unit,
    private val installedWidget: suspend () -> Boolean? = { null },
) {
    public suspend fun update(): Unit = updateWidget()
    /** Returns null when the platform cannot report whether the widget is installed. */
    public suspend fun isInstalled(): Boolean? = installedWidget()
}
