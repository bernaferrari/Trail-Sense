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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
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
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

@Composable
fun UnitSystemOnboardingScreen(
    useMetric: Boolean,
    onUnitSystemChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrailSenseSpacing.xl)
                .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.lg)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 12.dp,
                    bottomEnd = 32.dp,
                    bottomStart = 12.dp
                ),
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
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UnitPreviewValue(if (useMetric) "12.4" else "7.7", if (useMetric) "km" else "mi")
                UnitPreviewValue(if (useMetric) "21" else "70", if (useMetric) "C" else "F")
                UnitPreviewValue(if (useMetric) "1013" else "29.9", if (useMetric) "hPa" else "inHg")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = TrailSenseIcons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
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
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = page.image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).testTag("page_image")
                        )
                    }
                }
                Text(
                    text = page.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("page_name"),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
                OnboardingPageContents(
                    contents = page.contents,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.lg)
                )
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
