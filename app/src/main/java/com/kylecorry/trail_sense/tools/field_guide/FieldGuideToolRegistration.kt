package com.kylecorry.trail_sense.tools.field_guide

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
            R.id.fieldGuideFragment,
            ToolCategory.Books,
            settingsNavAction = R.id.fieldGuideSettingsFragment,
            additionalNavigationIds = listOf(
                R.id.fieldGuidePageFragment,
                R.id.fieldGuideSettingsFragment,
                R.id.sightingListFragment,
                R.id.createFieldGuideSightingFragment,
                R.id.createFieldGuidePageFragment
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
                        appNavigator.navigate(
                            R.id.fieldGuidePageFragment,
                            Bundle().apply {
                                fieldGuidePageId?.let {
                                    putLong("page_id", it)
                                }
                            }
                        )
                    },
                    geoJsonSource = ::FieldGuideSightingGeoJsonSource
                )
            )
        )
    }
}
