package com.kylecorry.trail_sense.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Pure 100% Compose Onboarding (no views, no bindings, no fragments).
 * Replaces the old OnboardingActivity XML + binding flow.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages = remember { OnboardingPages.getPages(context) }

    var currentPage by remember { mutableIntStateOf(0) }

    if (currentPage >= pages.size) {
        onFinished()
        return
    }

    val page = pages[currentPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(TrailSenseSpacing.xxl))

        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(TrailSenseSpacing.lg))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(TrailSenseSpacing.md))

        TrailSenseCard {
            Text(
                text = page.contents.toString(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(TrailSenseSpacing.md)
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                TrailSenseButton(
                    text = "Back",
                    onClick = { currentPage-- }
                )
            } else {
                Spacer(Modifier.size(1.dp))
            }

            TrailSenseButton(
                text = page.nextButtonText ?: "Next",
                onClick = {
                    currentPage++
                    if (currentPage >= pages.size) {
                        onFinished()
                    }
                }
            )
        }

        Spacer(Modifier.height(TrailSenseSpacing.md))
    }
}
