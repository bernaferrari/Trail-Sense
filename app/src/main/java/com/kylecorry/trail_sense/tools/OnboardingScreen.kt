package com.kylecorry.trail_sense.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

private val onboardingHeroShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 12.dp,
    bottomEnd = 32.dp,
    bottomStart = 12.dp
)

@Composable
fun UnitSystemOnboardingScreen(
    useMetric: Boolean,
    onUnitSystemChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.screen)
                    .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.screen)
            ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = onboardingHeroShape,
                modifier = Modifier.size(width = 88.dp, height = 76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = TrailSenseIcons.Filled.Hiking,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = TrailSenseSpacing.lg)
            )
            Text(
                text = stringResource(R.string.onboarding_setup_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TrailSenseSpacing.sm)
            )

            Text(
                text = stringResource(R.string.units),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xl)
            )
            Text(
                text = stringResource(R.string.units_setup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TrailSenseSpacing.xs)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween
                )
            ) {
                UnitSystemSegment(
                    title = stringResource(R.string.metric),
                    selected = useMetric,
                    onClick = { onUnitSystemChanged(true) },
                    index = 0
                )
                UnitSystemSegment(
                    title = stringResource(R.string.us_customary),
                    selected = !useMetric,
                    onClick = { onUnitSystemChanged(false) },
                    index = 1
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.lg),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TrailSenseSpacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UnitPreviewValue(if (useMetric) "12.4" else "7.7", if (useMetric) "km" else "mi")
                    UnitPreviewValue(if (useMetric) "21" else "70", if (useMetric) "C" else "F")
                    UnitPreviewValue(if (useMetric) "1013" else "29.9", if (useMetric) "hPa" else "inHg")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(TrailSenseSpacing.lg),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = TrailSenseIcons.Filled.PrivacyTip,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = TrailSenseSpacing.md)) {
                        Text(
                            text = stringResource(R.string.privacy_and_location),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.onboarding_private_by_design),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                        )
                    }
                }
            }

            TrailSenseButton(
                text = stringResource(R.string.next),
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.lg)
                    .testTag("next_button")
            )
            }
        }
    }
}

@Composable
private fun UnitPreviewValue(value: String, unit: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.UnitSystemSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = if (index == 0) {
            ButtonGroupDefaults.connectedLeadingButtonShapes()
        } else {
            ButtonGroupDefaults.connectedTrailingButtonShapes()
        },
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
        modifier = Modifier
            .weight(1f)
            .height(buttonHeight)
            .semantics { role = Role.RadioButton }
    ) {
        Text(text = title)
    }
}

@Composable
fun OnboardingScreen(
    page: OnboardingPage,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("coordinator"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.screen)
                    .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.lg),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = onboardingHeroShape,
                    modifier = Modifier.size(width = 88.dp, height = 76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = page.image,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp).testTag("page_image")
                        )
                    }
                }
                Text(
                    text = page.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.xl)
                        .testTag("page_name"),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.lg),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    OnboardingPageContents(
                        contents = page.contents,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TrailSenseSpacing.screen)
                    )
                }
            }

            TrailSenseButton(
                text = page.nextButtonText ?: stringResource(R.string.next),
                onClick = onNext,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.screen)
                    .padding(bottom = TrailSenseSpacing.screen)
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
    val markdown = remember { DependencyRegistry.get<MarkdownService>() }
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
        textAlign = TextAlign.Start
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
                image = TrailSenseIcons.Filled.Hiking
            ),
            onNext = {}
        )
    }
}
