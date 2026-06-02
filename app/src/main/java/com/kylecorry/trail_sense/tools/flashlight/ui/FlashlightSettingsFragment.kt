package com.kylecorry.trail_sense.tools.flashlight.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class FlashlightSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    val formatter = remember { FormatService.getInstance(requireContext()) }
                    val prefs = remember { UserPreferences(requireContext()).flashlight }
                    val hasFlashlight = remember {
                        FlashlightSubsystem.getInstance(requireContext()).isAvailable()
                    }
                    var toggleWithVolume by remember {
                        mutableStateOf(prefs.toggleWithVolumeButtons)
                    }
                    var shouldTimeout by remember { mutableStateOf(prefs.shouldTimeout) }
                    var timeout by remember { mutableStateOf(prefs.timeout) }
                    var controlScreenWithVolume by remember {
                        mutableStateOf(prefs.controlScreenFlashlightWithVolumeButtons)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.flashlight_title)
                                )
                            }
                            if (hasFlashlight) {
                                item("toggle_volume") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_flashlight_toggle_with_volume_title),
                                        summary = stringResource(R.string.pref_flashlight_toggle_with_volume_summary),
                                        checked = toggleWithVolume,
                                        onCheckedChange = {
                                            toggleWithVolume = it
                                            prefs.toggleWithVolumeButtons = it
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                                item("should_timeout") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_flashlight_should_timeout_title),
                                        checked = shouldTimeout,
                                        onCheckedChange = {
                                            shouldTimeout = it
                                            prefs.shouldTimeout = it
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                                item("timeout") {
                                    val title =
                                        stringResource(R.string.pref_flashlight_timeout_title)
                                    SettingsActionRow(
                                        label = title,
                                        summary = formatter.formatDuration(
                                            timeout,
                                            short = false,
                                            includeSeconds = true
                                        ),
                                        enabled = shouldTimeout,
                                        onClick = {
                                            CustomUiUtils.pickDuration(
                                                requireContext(),
                                                timeout,
                                                title
                                            ) {
                                                if (it != null && !it.isZero) {
                                                    prefs.timeout = it
                                                    timeout = it
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                }
                            }
                            item("screen_volume") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_flashlight_control_screen_with_volume_title),
                                    checked = controlScreenWithVolume,
                                    onCheckedChange = {
                                        controlScreenWithVolume = it
                                        prefs.controlScreenFlashlightWithVolumeButtons = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
