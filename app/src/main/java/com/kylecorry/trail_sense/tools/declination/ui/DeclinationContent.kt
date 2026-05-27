package com.kylecorry.trail_sense.tools.declination.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useLocation
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.extensions.compose.useState

/**
 * Compose conversion of ToolDeclinationFragment (Ralph Loop iteration 2).
 * Complex custom views are wrapped with AndroidView (established pattern).
 */
@Composable
fun Fragment.DeclinationContent(modifier: Modifier = Modifier) {
    val formatService = useService<FormatService>()

    val (initialLocation, isLocationUpToDate) = useLocation()
    val (location, setLocation) = useState(initialLocation)

    val declination = useMemo(location) {
        location?.let { Geophysics.getGeomagneticDeclination(it) }
    }

    val titleText = useMemo(declination, formatService) {
        declination?.let { value ->
            if (kotlin.math.abs(value) < 0.05f) {
                formatService.formatDegrees(value, 1)
            } else {
                val dir = if (value < 0) "W" else "E"
                "${formatService.formatDegrees(value, 1)} ($dir)"
            }
        } ?: getString(R.string.loading)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        AndroidView(
            factory = { ctx ->
                com.kylecorry.trail_sense.shared.views.CoordinateInputView(ctx).apply {
                    coordinate = initialLocation
                    setOnCoordinateChangeListener { newCoord ->
                        if (newCoord != null) setLocation(newCoord)
                    }
                }
            },
            update = { view ->
                if (isLocationUpToDate) view.coordinate = location
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (!isLocationUpToDate) {
            CircularProgressIndicator()
        }

        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    DeclinationView(ctx).apply {
                        this.declination = declination ?: 0f
                    }
                },
                update = { view ->
                    view.declination = declination ?: 0f
                }
            )
        }
    }

    useEffect(initialLocation, isLocationUpToDate) {
        if (isLocationUpToDate) {
            setLocation(initialLocation)
        }
    }
}
