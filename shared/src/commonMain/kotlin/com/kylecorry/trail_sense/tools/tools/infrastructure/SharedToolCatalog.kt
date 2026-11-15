package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.ballistics
import com.kylecorry.trail_sense.shared.generated.resources.convert
import com.kylecorry.trail_sense.shared.generated.resources.survival_guide
import com.kylecorry.trail_sense.shared.generated.resources.tool_ruler_title
import com.kylecorry.trail_sense.shared.generated.resources.tool_notes_title
import com.kylecorry.trail_sense.shared.generated.resources.tool_bubble_level_title
import com.kylecorry.trail_sense.shared.generated.resources.tool_bubble_level_summary
import com.kylecorry.trail_sense.shared.generated.resources.tool_lightning_title
import com.kylecorry.trail_sense.shared.generated.resources.tool_lightning_description
import com.kylecorry.trail_sense.shared.icons.trail.Ballistics
import com.kylecorry.trail_sense.shared.icons.trail.ToolDistanceConvert
import com.kylecorry.trail_sense.shared.icons.trail.ToolNotes
import com.kylecorry.trail_sense.shared.icons.trail.Level
import com.kylecorry.trail_sense.shared.icons.trail.SurvivalGuide
import com.kylecorry.trail_sense.shared.icons.trail.Ruler
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts

/** Tools whose screens and services are available in the shared app. */
fun sharedTools(strings: AppStrings, hasMotion: Boolean = false): List<SharedTool> = listOfNotNull(
    SharedTool(
        ToolIds.SURVIVAL_GUIDE, strings.get(Res.string.survival_guide), TrailIcons.SurvivalGuide,
        Route.SurvivalGuideList(), ToolCategory.Books, guideId = RawTexts.guide_tool_survival_guide,
        additionalRoutes = listOf(Route.SurvivalGuideReader::class)
    ),
    SharedTool(
        ToolIds.RULER, strings.get(Res.string.tool_ruler_title), TrailIcons.Ruler,
        Route.Ruler, ToolCategory.Distance, guideId = RawTexts.guide_tool_ruler,
        settingsRoute = Route.RulerSettings
    ),
    SharedTool(
        ToolIds.NOTES, strings.get(Res.string.tool_notes_title), TrailIcons.ToolNotes,
        Route.Notes, ToolCategory.Other, guideId = RawTexts.guide_tool_notes,
        additionalRoutes = listOf(Route.NoteEditor::class)
    ),
    SharedTool(
        ToolIds.CONVERT, strings.get(Res.string.convert), TrailIcons.ToolDistanceConvert,
        Route.Convert, ToolCategory.Other, guideId = RawTexts.guide_tool_convert
    ),
    SharedTool(
        ToolIds.BALLISTICS, strings.get(Res.string.ballistics), TrailIcons.Ballistics,
        Route.Ballistics, ToolCategory.Other, guideId = RawTexts.guide_tool_ballistics
    ),
    SharedTool(
        ToolIds.LIGHTNING_STRIKE_DISTANCE, strings.get(Res.string.tool_lightning_title), TrailIcons.TorchOn,
        Route.Lightning, ToolCategory.Weather, description = strings.get(Res.string.tool_lightning_description),
        guideId = RawTexts.guide_tool_lightning_strike_distance
    ),
    if (hasMotion) SharedTool(
        ToolIds.BUBBLE_LEVEL, strings.get(Res.string.tool_bubble_level_title), TrailIcons.Level,
        Route.Level, ToolCategory.Angles, description = strings.get(Res.string.tool_bubble_level_summary),
        guideId = RawTexts.guide_tool_bubble_level
    ) else null
)
