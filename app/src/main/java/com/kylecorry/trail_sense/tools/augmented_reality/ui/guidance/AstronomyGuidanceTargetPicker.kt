package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
            val notVisible = view.context.getString(R.string.not_visible)

            val options = mutableListOf<AstronomyGuidanceOption>()

            val sunAltitude = astronomyService.getSunPosition(location, time).altitude
            if (drawBelowHorizon || sunAltitude > 0f) {
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Sun, getDisplayedTime),
                        view.context.getString(R.string.sun),
                        if (sunAltitude > 0f) null else notVisible,
                        R.drawable.ic_sun,
                        sortOrder = if (sunAltitude > 0f) 0 else 2
                    )
                )
            }

            val moonAltitude = astronomyService.getMoonPosition(location, time).altitude
            if (drawBelowHorizon || moonAltitude > 0f) {
                val phase = astronomyService.getMoonPhase(time)
                options.add(
                    AstronomyGuidanceOption(
                        AstronomyGuidanceTarget(astronomyService, AstronomySelection.Moon, getDisplayedTime),
                        view.context.getString(R.string.moon),
                        if (moonAltitude > 0f) null else notVisible,
                        R.drawable.ic_moon,
                        iconBitmap = MoonPhaseImageMapper(view.context).getPhaseImage(
                            phase.phaseAngle,
                            Resources.dp(view.context, 24f).toInt(),
                            Resources.dp(view.context, 24f).toInt(),
                            astronomyService.getMoonTilt(location, time)
                        ),
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
                            AstronomySelection.MeteorShowerTarget(it.first),
                            getDisplayedTime
                        ),
                        it.first.readableName(),
                        if (it.second.altitude > 0f) null else notVisible,
                        R.drawable.ic_meteor,
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
                                AstronomySelection.PlanetTarget(it.first),
                                getDisplayedTime
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
                                AstronomySelection.StarTarget(it.first),
                                getDisplayedTime
                            ),
                            "${view.context.getString(R.string.star)}: ${it.first.name}",
                            if (it.second.second > 0f) null else notVisible,
                            R.drawable.ic_star,
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
    ): AstronomyGuidanceOption? = suspendCoroutine { cont ->
        val dialogView = createSearchListDialog(view.context)
        val search = dialogView.findViewById<SearchView>(R.id.search)
        val list = dialogView.findViewById<ComposeView>(R.id.list)
        val empty = dialogView.findViewById<TextView>(R.id.empty_text)
        var selected: AstronomyGuidanceOption? = null
        var resumed = false
        var filteredOptions by mutableStateOf(options)

        empty.text = view.context.getString(R.string.no_results)
        empty.isVisible = options.isEmpty()

        val alert = Alerts.dialog(
            view.context,
            view.context.getString(R.string.locate),
            contentView = dialogView,
            okText = null
        ) {
            if (!resumed) {
                resumed = true
                cont.resume(selected)
            }
        }

        list.setContent {
            TrailSenseTheme {
                LazyColumn(
                    modifier = Modifier.testTag("astronomy_guidance_target_list")
                ) {
                    items(filteredOptions, key = { it.name }) { option ->
                        TrailSenseListItem(
                            title = option.name,
                            subtitle = option.subtitle,
                            icon = painterResource(option.icon),
                            iconTint = option.iconTint?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.primary,
                            iconRotation = option.iconRotation,
                            onClick = {
                                selected = option
                                if (!resumed) {
                                    resumed = true
                                    cont.resume(option)
                                }
                                alert.dismiss()
                            },
                            animateEntrance = false,
                            modifier = Modifier.testTag("astronomy_guidance_target")
                        )
                        TrailSenseDivider()
                    }
                }
            }
        }

        search.setOnSearchListener { query ->
            val filtered = if (query.isBlank()) {
                options
            } else {
                TextUtils.search(query, options) {
                    listOfNotNull(it.name, it.subtitle)
                }
            }
            filteredOptions = filtered
            empty.isVisible = filtered.isEmpty()
        }
    }

    private fun createSearchListDialog(context: Context): View {
        val horizontalMargin = Resources.dp(context, 16f).toInt()
        val emptyTopPadding = Resources.dp(context, 32f).toInt()
        val listTopMargin = Resources.dp(context, 16f).toInt()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            SearchView(context, null).apply {
                id = R.id.search
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = horizontalMargin
                marginEnd = horizontalMargin
            }
        )

        root.addView(
            TextView(context).apply {
                id = R.id.empty_text
                gravity = Gravity.CENTER
                setPadding(0, emptyTopPadding, 0, 0)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                isVisible = false
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        root.addView(
            ComposeView(context).apply {
                id = R.id.list
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = listTopMargin
            }
        )

        return root
    }

    private data class AstronomyGuidanceOption(
        val target: ARGuidanceTarget,
        val name: String,
        val subtitle: String? = null,
        val icon: Int,
        val iconBitmap: android.graphics.Bitmap? = null,
        val iconTint: Int? = null,
        val iconRotation: Float = 0f,
        val sortOrder: Int
    )

    private suspend fun pick(view: AugmentedRealityView, options: List<AstronomyGuidanceOption>): ARGuidanceTarget? {
        return pickGuidanceOption(view, options)?.target
    }
}
