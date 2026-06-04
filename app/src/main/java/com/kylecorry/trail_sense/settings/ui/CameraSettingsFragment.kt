package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class CameraSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()).camera }

    @Composable
    override fun FragmentContent() {
        val projectionNames = projectionTypeNames()
        val projectionTypes = projectionNames.keys.toList()
        var projectionType by remember { mutableStateOf(prefs.projectionType) }
        val title = stringResource(R.string.projection_type)
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
                    TrailSenseSectionHeader(title = stringResource(R.string.camera))
                }
                item("projection") {
                    SettingsActionRow(
                        label = title,
                        summary = projectionNames.getValue(projectionType),
                        onClick = {
                            Pickers.item(
                                requireContext(),
                                title,
                                projectionNames.values.toList(),
                                projectionTypes.indexOf(projectionType)
                            ) {
                                if (it != null) {
                                    projectionType = projectionTypes[it]
                                    prefs.projectionType = projectionTypes[it]
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
    }

    private fun projectionTypeNames(): Map<CameraPreferences.ProjectionType, String> {
        return mapOf(
            CameraPreferences.ProjectionType.EstimatedIntrinsics to
                    "${getString(R.string.projection_estimated_intrinsics)} (${
                        getString(R.string.default_string)
                    })",
            CameraPreferences.ProjectionType.ManufacturerIntrinsics to
                    getString(R.string.projection_manufacturer_intrinsics),
            CameraPreferences.ProjectionType.Perspective to getString(R.string.projection_perspective),
            CameraPreferences.ProjectionType.Linear to getString(R.string.projection_linear)
        )
    }
}
