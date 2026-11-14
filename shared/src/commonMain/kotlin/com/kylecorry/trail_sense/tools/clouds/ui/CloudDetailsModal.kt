@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.images.rememberAppImage
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.dialogs.showComposeDialog
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.clouds.domain.CloudService

/**
 * A cloud's field-guide card: its picture, what it looks like, then what it means for the weather as grouped facts
 * (forecast, chance of precipitation, what may fall), so the answer to "should I worry?" is one glance away.
 */
class CloudDetailsModal(private val host: AppHost, private val strings: AppStrings) {

    private val cloudService = CloudService()
    private val details = CloudDetailsText(strings)
    private val formatter = FormatService.getInstance()

    fun show(cloud: CloudGenus?) {
        val precipitation = cloudService.getPrecipitation(cloud)
        val info = CloudDetailsInfo(
            name = details.getCloudName(cloud),
            description = details.getCloudDescription(cloud),
            forecast = details.getCloudForecast(cloud),
            precipitationChance = strings.get(
                Res.string.precipitation_chance,
                formatter.formatProbability(cloudService.getPrecipitationProbability(cloud))
            ),
            precipitationTypes = if (precipitation.isEmpty()) {
                strings.get(Res.string.precipitation_none)
            } else {
                precipitation.joinToString(" · ") { formatter.formatPrecipitation(it) }
            },
            imagePath = details.getCloudImagePath(cloud)
        )

        showComposeDialog(host) { dismiss ->
            CloudDetailsContent(info = info, onDismiss = dismiss)
        }
    }
}

private class CloudDetailsInfo(
    val name: String,
    val description: String,
    val forecast: String,
    val precipitationChance: String,
    val precipitationTypes: String,
    /** The photo to show, or null for a clear sky (shown as the sky's colour). */
    val imagePath: String?
)

@Composable
private fun ColumnScope.CloudDetailsContent(info: CloudDetailsInfo, onDismiss: () -> Unit) {
    val bitmap = rememberAppImage(info.imagePath)

    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TrailSenseSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        // Clear skies show the sky's colour; a cloud's photo fills in once decoded.
        if (info.imagePath == null || bitmap != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (info.imagePath == null) {
                    Color(CloudDetailsText.CLEAR_SKY_COLOR)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = info.name,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TrailSenseRowGroup {
            row { position ->
                TrailSenseListItem(
                    title = info.forecast,
                    icon = rememberVectorPainter(TrailIcons.Barometer),
                    position = position
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = info.precipitationChance,
                    subtitle = info.precipitationTypes,
                    icon = rememberVectorPainter(TrailIcons.Precipitation),
                    position = position
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TrailSenseSpacing.lg),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) {
            Text(systemString(SystemString.Ok), fontWeight = FontWeight.SemiBold)
        }
    }
}
