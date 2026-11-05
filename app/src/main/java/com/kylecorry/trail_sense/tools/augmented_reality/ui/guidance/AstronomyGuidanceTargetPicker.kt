package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.dialogs.TrailSenseDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.icons.trail.Search
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZonedDateTime
import kotlin.coroutines.resume

class AstronomyGuidanceTargetPicker(
    private val astronomyService: AstronomyService,
    private val drawBelowHorizon: Boolean,
    private val drawStars: Boolean,
    private val drawLowBrightnessObjects: Boolean,
    private val getDisplayedTime: () -> ZonedDateTime
) {

    suspend fun pick(view: AugmentedRealityView): ARGuidanceTarget? {
        val options = getGuidanceOptions(view)
        return pick(view, options)
    }

    private suspend fun getGuidanceOptions(view: AugmentedRealityView): List<AstronomyGuidanceOption> =
        onDefault {
            val location = view.location
            val time = getDisplayedTime()
            val planetMapper = PlanetMapper(view.context)
            val moonPhaseMapper = MoonPhaseImageMapper()
            val notVisible = view.context.getString(R.string.not_visible)

            val options = mutableListOf<AstronomyGuidanceOption>()

            val sunAltitude = astronomyService.getSunAltitude(location, time)
            if (drawBelowHorizon || sunAltitude > 0f) {
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Sun),
                        view.context.getString(R.string.sun),
                        if (sunAltitude > 0f) null else notVisible,
                        TrailIcons.Sun,
                        sortOrder = if (sunAltitude > 0f) 0 else 2
                    )
                )
            }

            val moonAltitude = astronomyService.getMoonAltitude(location, time)
            if (drawBelowHorizon || moonAltitude > 0f) {
                val phase = Astronomy.getMoonPhase(time)
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Moon),
                        view.context.getString(R.string.moon),
                        if (moonAltitude > 0f) null else notVisible,
                        moonPhaseMapper.getPhaseImage(phase.phase),
                        iconRotation = astronomyService.getMoonTilt(location, time).parallacticAngle,
                        sortOrder = if (moonAltitude > 0f) 1 else 3
                    )
                )
            }

            astronomyService.getVisibleMeteorShowers(
                location,
                time,
                if (drawBelowHorizon) null else -10f
            ).forEach {
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(
                            astronomyService,
                            AstronomySelection.MeteorShowerTarget(it.first)
                        ),
                        it.first.readableName(),
                        if (it.second.altitude > 0f) null else notVisible,
                        TrailIcons.Meteor,
                        sortOrder = if (it.second.altitude > 0f) 4 else 5
                    )
                )
            }

            if (drawStars) {
                astronomyService.getVisiblePlanets(
                    location,
                    time,
                    if (drawBelowHorizon) null else 0f,
                    includeDimPlanets = drawLowBrightnessObjects
                ).forEach {
                    options.add(
                        AstronomyGuidanceOption(
                            AstronomyGuidanceTarget(
                                astronomyService,
                                AstronomySelection.PlanetTarget(it.first)
                            ),
                            planetMapper.getName(it.first),
                            if (it.second.altitude > 0f) null else notVisible,
                            planetMapper.getImage(it.first),
                            sortOrder = if (it.second.altitude > 0f) 6 else 7
                        )
                    )
                }

                astronomyService.getVisibleStars(
                    location,
                    time,
                    if (drawBelowHorizon) null else 0f,
                    maxMagnitude = if (drawLowBrightnessObjects) null else 4.0f
                ).forEach {
                    options.add(
                        AstronomyGuidanceOption(
                            AstronomyGuidanceTarget(
                                astronomyService,
                                AstronomySelection.StarTarget(it.first)
                            ),
                            "${view.context.getString(R.string.star)}: ${it.first.name}",
                            if (it.second.second > 0f) null else notVisible,
                            TrailIcons.Star,
                            iconTint = Colors.fromColorTemperature(
                                Astronomy.getColorTemperature(it.first)
                            ),
                            sortOrder = if (it.second.second > 0f) 8 else 9
                        )
                    )
                }
            }

            options.sortedWith(compareBy<AstronomyGuidanceOption> { it.sortOrder }.thenBy { it.name })
        }

    private suspend fun pickGuidanceOption(
        view: AugmentedRealityView,
        options: List<AstronomyGuidanceOption>
    ): AstronomyGuidanceOption? = suspendCancellableCoroutine { cont ->
        val context = view.context
        var selected: AstronomyGuidanceOption? = null
        var resumed = false
        var alert: TrailSenseDialog? = null
        alert = Alerts.dialog(
            context,
            context.getString(R.string.locate),
            okText = null,
            body = {
            AstronomyGuidanceTargetPickerContent(
                options = options,
                onSelected = {
                selected = it
                if (!resumed) {
                    resumed = true
                    cont.resume(it)
                }
                alert?.dismiss()
                }
            )
            }
        ) {
            if (!resumed) {
                resumed = true
                cont.resume(selected)
            }
        }
    }

    @Composable
    private fun AstronomyGuidanceTargetPickerContent(
        options: List<AstronomyGuidanceOption>,
        onSelected: (AstronomyGuidanceOption) -> Unit
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var filteredOptions by remember(options) { mutableStateOf(options) }
        var query by remember { mutableStateOf("") }

        Column {
            TrailSenseInputField(
                value = query,
                label = context.getString(R.string.search),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                onValueChange = {
                    query = it
                    filteredOptions = if (it.isBlank()) {
                        options
                    } else {
                        TextUtils.search(it, options) { option ->
                            listOfNotNull(option.name, option.subtitle)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg),
                leadingIcon = {
                    Icon(TrailIcons.Search, contentDescription = null)
                }
            )

            if (filteredOptions.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.lg)
                        .testTag("astronomy_guidance_target_list"),
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    itemsIndexed(filteredOptions, key = { _, it -> it.name }) { index, option ->
                        TrailSenseListItem(
                            title = option.name,
                            subtitle = option.subtitle,
                            icon = rememberVectorPainter(option.icon),
                            iconTint = option.iconTint?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.primary,
                            iconRotation = option.iconRotation,
                            onClick = { onSelected(option) },
                            animateEntrance = false,
                            position = TrailSenseGroupPosition.at(index, filteredOptions.size),
                            modifier = Modifier.testTag("astronomy_guidance_target")
                        )
                    }
                }
            }
        }
    }

    private data class AstronomyGuidanceOption(
        val target: ARGuidanceTarget,
        val name: String,
        val subtitle: String? = null,
        val icon: ImageVector,
        val iconTint: Int? = null,
        val iconRotation: Float = 0f,
        val sortOrder: Int
    )

    private suspend fun pick(view: AugmentedRealityView, options: List<AstronomyGuidanceOption>): ARGuidanceTarget? {
        return pickGuidanceOption(view, options)?.target
    }
}
