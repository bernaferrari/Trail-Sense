package com.kylecorry.trail_sense.tools.field_guide

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.map_layers.FieldGuideSightingGeoJsonSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object FieldGuideToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.FIELD_GUIDE,
            context.getString(R.string.field_guide),
            TrailIcons.FieldGuide,
            Route.FieldGuide,
            ToolCategory.Books,
            settingsRoute = Route.FieldGuideSettings,
            additionalRoutes = listOf(
                Route.FieldGuidePage::class,
                Route.FieldGuideSettings::class,
                Route.SightingList::class,
                Route.CreateFieldGuideSighting::class,
                Route.CreateFieldGuidePage::class
            ),
            guideId = R.raw.guide_tool_field_guide,
            singletons = listOf(
                FieldGuideRepo::getInstance
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    FieldGuideSightingGeoJsonSource.SOURCE_ID,
                    context.getString(R.string.sightings),
                    description = context.getString(R.string.map_layer_field_guide_sightings_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = FieldGuideSightingGeoJsonSource.PREFERENCE_SHOW_IMAGES,
                            title = context.getString(R.string.show_images),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = false
                        )
                    ),
                    openFeature = { feature, _, appNavigator ->
                        val fieldGuidePageId =
                            feature.getLongProperty(FieldGuideSightingGeoJsonSource.PROPERTY_PAGE_ID)
                        appNavigator.navigate(Route.FieldGuidePage(pageId = fieldGuidePageId ?: 0L))
                    },
                    geoJsonSource = ::FieldGuideSightingGeoJsonSource
                )
            )
        )
    }
}
