package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.launch

@Composable
fun Fragment.SightingListContent(modifier: Modifier = Modifier) {
    // Clean modern placeholder — full expressive sighting list will be a real Nav3 screen soon
    Box(modifier = modifier.fillMaxSize()) {
        TrailSenseEmptyState(
            title = "Sightings",
            message = "Sighting management is being migrated to the new modern Compose + design system experience.\n\nCore Field Guide pages work beautifully in the modern path today."
        )
    }
}
                            Text(
                                text = sighting.timestamp.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${sighting.coordinate.latitude}, ${sighting.coordinate.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val bundle = Bundle().apply {
                    putLong("page_id", pageId)
                }
                navController.navigate(R.id.createFieldGuideSightingFragment, bundle)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = getString(R.string.create))
        }
    }
}

class SightingListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SightingListContent()
    }
}

