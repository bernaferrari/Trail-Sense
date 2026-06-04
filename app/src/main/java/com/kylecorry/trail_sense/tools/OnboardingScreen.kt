package com.kylecorry.trail_sense.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

@Composable
fun OnboardingScreen(
    page: OnboardingPage,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("coordinator")
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = page.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("page_name"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Image(
                    painter = painterResource(page.image),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.lg)
                        .size(100.dp)
                        .testTag("page_image")
                )

                TrailSenseCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.lg)
                ) {
                    OnboardingPageContents(
                        contents = page.contents,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            TrailSenseButton(
                text = page.nextButtonText ?: stringResource(R.string.next),
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(bottom = TrailSenseSpacing.lg)
                    .testTag("next_button")
            )
        }
    }
}

@Composable
private fun OnboardingPageContents(
    contents: CharSequence,
    modifier: Modifier = Modifier
) {
    val markdown = remember { AppServiceRegistry.get<MarkdownService>() }
    val parsed = remember(contents, markdown) {
        if (contents is String) {
            markdown.toMarkdown(contents)
        } else {
            contents
        }
    }
    TrailSenseLinkedText(
        text = parsed,
        modifier = modifier.testTag("page_contents"),
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
        ),
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    val context = LocalContext.current
    TrailSenseTheme {
        OnboardingScreen(
            page = OnboardingPage(
                title = context.getString(R.string.explore),
                contents = context.getString(R.string.onboarding_explore),
                image = R.drawable.steps
            ),
            onNext = {}
        )
    }
}
