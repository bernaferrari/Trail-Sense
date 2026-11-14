package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.widgets.Widgets
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.AndroidMapLayerDefinition
import com.kylecorry.trail_sense.shared.quickactions.QuickActionOpenTool
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityToolRegistration
import com.kylecorry.trail_sense.tools.ballistics.BallisticsToolRegistration
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.beacons.BeaconsToolRegistration
import com.kylecorry.trail_sense.tools.cliffheight.CliffHeightToolRegistration
import com.kylecorry.trail_sense.tools.climate.ClimateToolRegistration
import com.kylecorry.trail_sense.tools.clinometer.ClinometerToolRegistration
import com.kylecorry.trail_sense.tools.clock.ClockToolRegistration
import com.kylecorry.trail_sense.tools.clouds.CloudsToolRegistration
import com.kylecorry.trail_sense.tools.comms.messaging.LocalMessagingToolRegistration
import com.kylecorry.trail_sense.tools.comms.talk.LocalTalkToolRegistration
import com.kylecorry.trail_sense.tools.convert.ConvertToolRegistration
import com.kylecorry.trail_sense.tools.declination.DeclinationToolRegistration
import com.kylecorry.trail_sense.tools.diagnostics.DiagnosticsToolRegistration
import com.kylecorry.trail_sense.tools.experimentation.ExperimentationToolRegistration
import com.kylecorry.trail_sense.tools.field_guide.FieldGuideToolRegistration
import com.kylecorry.trail_sense.tools.flashlight.FlashlightToolRegistration
import com.kylecorry.trail_sense.tools.guide.UserGuideToolRegistration
import com.kylecorry.trail_sense.tools.level.BubbleLevelToolRegistration
import com.kylecorry.trail_sense.tools.light.LightMeterToolRegistration
import com.kylecorry.trail_sense.tools.lightning.LightningStrikeDistanceToolRegistration
import com.kylecorry.trail_sense.tools.magnifier.MagnifierToolRegistration
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.metaldetector.MetalDetectorToolRegistration
import com.kylecorry.trail_sense.tools.mirror.MirrorCameraToolRegistration
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.notes.NotesToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.packs.PackingListsToolRegistration
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.qr.QRCodeScannerToolRegistration
import com.kylecorry.trail_sense.tools.ruler.RulerToolRegistration
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.signal_finder.SignalFinderToolRegistration
import com.kylecorry.trail_sense.tools.solarpanel.SolarPanelAlignerToolRegistration
import com.kylecorry.trail_sense.tools.survival_guide.SurvivalGuideToolRegistration
import com.kylecorry.trail_sense.tools.temperature_estimation.TemperatureEstimateToolRegistration
import com.kylecorry.trail_sense.tools.tides.TidesToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.triangulate.TriangulateLocationToolRegistration
import com.kylecorry.trail_sense.tools.turn_back.TurnBackToolRegistration
import com.kylecorry.trail_sense.tools.waterpurification.WaterBoilTimerToolRegistration
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.whistle.WhistleToolRegistration
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

object Tools {

    private val hooks = Hooks()
    private val registry = listOf(
        FlashlightToolRegistration,
        WhistleToolRegistration,
        RulerToolRegistration,
        PedometerToolRegistration,
        CliffHeightToolRegistration,
        NavigationToolRegistration,
        BeaconsToolRegistration,
        OfflineMapsToolRegistration,
        PathsToolRegistration,
        TriangulateLocationToolRegistration,
        ClinometerToolRegistration,
        BubbleLevelToolRegistration,
        ClockToolRegistration,
        AstronomyToolRegistration,
        WaterBoilTimerToolRegistration,
        TidesToolRegistration,
        BatteryToolRegistration,
        SolarPanelAlignerToolRegistration,
        LightMeterToolRegistration,
        WeatherToolRegistration,
        ClimateToolRegistration,
        TemperatureEstimateToolRegistration,
        CloudsToolRegistration,
        LightningStrikeDistanceToolRegistration,
        AugmentedRealityToolRegistration,
        ConvertToolRegistration,
        PackingListsToolRegistration,
        MetalDetectorToolRegistration,
        WhiteNoiseToolRegistration,
        NotesToolRegistration,
        QRCodeScannerToolRegistration,
        SensorsToolRegistration,
        DiagnosticsToolRegistration,
        SettingsToolRegistration,
        UserGuideToolRegistration,
        ExperimentationToolRegistration,
        MirrorCameraToolRegistration,
        TurnBackToolRegistration,
        LocalMessagingToolRegistration,
        LocalTalkToolRegistration,
        SurvivalGuideToolRegistration,
        FieldGuideToolRegistration,
        SignalFinderToolRegistration,
        BallisticsToolRegistration,
        DeclinationToolRegistration,
        MapToolRegistration,
        MagnifierToolRegistration
    )


    fun isToolAvailable(context: Context, toolId: Long): Boolean {
        return getTool(context, toolId) != null
    }

    fun getTool(context: Context, toolId: Long): Tool? {
        return getTools(context).firstOrNull { it.id == toolId }
    }

    fun getTools(context: Context, availableOnly: Boolean = true): List<Tool> {
        val tools = hooks.memo("tools", Resources.getLocale(context).language) {
            registry.map { it.getTool(context.applicationContext) }
        }


        return tools.filter { !availableOnly || it.isAvailable(context) }
    }

    fun getMapLayerDefinition(context: Context, sourceId: String): AndroidMapLayerDefinition? {
        return getTools(context).flatMap { it.mapLayers }.firstOrNull { it.id == sourceId }
    }

    fun getQuickActions(context: Context): List<ToolQuickAction> {
        val none = ToolQuickAction(
            ToolIds.QUICK_ACTION_NONE,
            context.getString(R.string.none).capitalizeWords()
        ) { button, host ->
            QuickActionNone(button, host)
        }

        val tools = getTools(context)

        val sortedTools = AlphabeticalToolSort().sort(tools).flatMap { it.tools }

        val quickActions = tools
            .flatMap { it.quickActions }
            .distinctBy { it.id }
            .sortedBy { it.name }
            .map { it.copy(name = it.name.capitalizeWords()) }

        val toolActions = sortedTools.map {
            ToolQuickAction(
                it.id.toInt() + ToolIds.TOOL_QUICK_ACTION_OFFSET, // Avoid overlap
                context.getString(R.string.tool_quick_action_name, it.name)
            ) { button, host -> QuickActionOpenTool(button, host, it.route, it.icon) }
        }

        return listOf(none) + quickActions + toolActions
    }

    fun getService(context: Context, serviceId: String): ToolService? {
        return getTools(context)
            .flatMap { it.services }
            .firstOrNull { it.id == serviceId }
    }

    fun getWidgetFromAppWidgetId(context: Context, appWidgetId: Int): ToolWidget? {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider ?: return null
        return getTools(context)
            .flatMap { it.widgets }
            .firstOrNull {
                it.widgetClass == componentName.className.let { cls ->
                    tryOrDefault(null) {
                        Class.forName(cls)
                    }
                }
            }
    }

    fun getWidget(context: Context, widgetId: String): ToolWidget? {
        return getTools(context)
            .flatMap { it.widgets }
            .firstOrNull { it.id == widgetId }
    }

    fun triggerWidgetUpdate(context: Context, widgetId: String?) {
        if (widgetId == null) {
            // Update all widgets
            // TODO: Also update in app widgets?
            getTools(context)
                .flatMap { it.widgets }
                .forEach { Widgets.requestUpdate(context, it.widgetClass) }
            return
        }

        val widget = getWidget(context, widgetId) ?: return
        Widgets.requestUpdate(context, widget.widgetClass)
        ToolBroadcasts.broadcast(
            SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET,
            EventData().apply {
                putString("widgetId", widgetId)
            }
        )
    }

    fun hasAnyWidgetsOnHomeScreen(
        context: Context,
        predicate: (widget: ToolWidget) -> Boolean = { true }
    ): Boolean {
        return getTools(context)
            .flatMap { it.widgets }
            .filter(predicate)
            .any { isWidgetOnHomeScreen(context, it) }
    }

    private fun isWidgetOnHomeScreen(context: Context, widget: ToolWidget): Boolean {
        return AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, widget.widgetClass))
            .isNotEmpty()
    }
}
