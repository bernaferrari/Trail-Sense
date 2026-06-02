package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

class PrivacySettingsFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PrivacySettingsScreen(
                        onNavigateToGpsCalibration = {
                            findNavController().navigateWithAnimation(
                                R.id.action_privacy_settings_to_gps_calibration
                            )
                        },
                        onScreenshotProtectionChanged = { isProtected ->
                            prefs.privacy.isScreenshotProtectionOn = isProtected
                            Screen.setAllowScreenshots(requireActivity().window, !isProtected)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun PrivacySettingsScreen(
        onNavigateToGpsCalibration: () -> Unit,
        onScreenshotProtectionChanged: (Boolean) -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var isLocationMocked by remember { mutableStateOf(isLocationMocked()) }
        var isScreenshotProtectionOn by remember {
            mutableStateOf(prefs.privacy.isScreenshotProtectionOn)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isLocationMocked = isLocationMocked()
                    isScreenshotProtectionOn = prefs.privacy.isScreenshotProtectionOn
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
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
                    TrailSenseSectionHeader(title = stringResource(R.string.privacy))
                }
                item("location") {
                    TrailSenseListItem(
                        title = stringResource(R.string.location),
                        subtitle = if (isLocationMocked) {
                            stringResource(R.string.location_mocked)
                        } else {
                            stringResource(R.string.location_not_mocked)
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToGpsCalibration,
                        animateEntrance = false
                    )
                    TrailSenseDivider()
                }
                item("screenshot_protection") {
                    TrailSenseSwitchRow(
                        label = stringResource(R.string.pref_privacy_screenshot_protection_title),
                        checked = isScreenshotProtectionOn,
                        onCheckedChange = {
                            isScreenshotProtectionOn = it
                            onScreenshotProtectionChanged(it)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
            }
        }
    }

    private fun isLocationMocked(): Boolean {
        return !prefs.useAutoLocation || !SensorService(requireContext()).hasLocationPermission()
    }

}
