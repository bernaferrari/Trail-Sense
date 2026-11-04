package com.kylecorry.trail_sense.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseIconWell
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
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

/**
 * The first screen: a small landscape drawn in the app's own colors, the app's promise, the unit system (with a live
 * preview of what it changes) and the privacy promise. Next sits at the bottom on every onboarding page.
 */
@Composable
fun UnitSystemOnboardingScreen(
    useMetric: Boolean,
    onUnitSystemChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingFrame(
        nextText = stringResource(R.string.next),
        onNext = onNext,
        modifier = modifier
    ) {
        TrailHero(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = TrailSenseSpacing.xl)
        )
        Text(
            text = stringResource(R.string.onboarding_setup_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = TrailSenseSpacing.sm)
        )

        TrailSenseGroupHeader(
            stringResource(R.string.units),
            modifier = Modifier.padding(top = TrailSenseSpacing.lg)
        )
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            TrailSenseGroupCard(TrailSenseGroupPosition(isFirst = true, isLast = false)) {
                TrailSenseConnectedChoice(
                    options = listOf(
                        TrailSenseChoice(true, stringResource(R.string.metric)),
                        TrailSenseChoice(false, stringResource(R.string.us_customary))
                    ),
                    selected = useMetric,
                    onSelect = onUnitSystemChanged,
                    label = stringResource(R.string.units)
                )
            }
            // What the choice changes, as three readings that roll over to the other system.
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                val cells = listOf(
                    Triple(if (useMetric) "12.4" else "7.7", if (useMetric) "km" else "mi", stringResource(R.string.distance)),
                    Triple(if (useMetric) "21°" else "70°", if (useMetric) "C" else "F", stringResource(R.string.temperature)),
                    Triple(if (useMetric) "1013" else "29.9", if (useMetric) "hPa" else "inHg", stringResource(R.string.pressure))
                )
                cells.forEachIndexed { index, (value, unit, label) ->
                    UnitPreviewValue(
                        value = value,
                        unit = unit,
                        label = label,
                        metric = useMetric,
                        shape = trailSenseGridCellShape(index, cells.size, 1, 2),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.units_setup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.lg, top = TrailSenseSpacing.sm)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.xl),
            shape = RoundedCornerShape(TrailSenseShape.groupOuter),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(TrailSenseSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                TrailSenseIconWell(trailSenseWellColors(TrailSenseAccents.Leaf)) {
                    Icon(TrailSenseIcons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = stringResource(R.string.privacy_and_location),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.onboarding_private_by_design),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Scrolling content with the page's action pinned at the bottom, centered at a readable width. */
@Composable
private fun OnboardingFrame(
    nextText: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
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
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.lg),
                content = content
            )
            TrailSenseButton(
                text = nextText,
                onClick = onNext,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .padding(bottom = TrailSenseSpacing.lg)
                    .testTag("next_button")
            )
        }
    }
}

/**
 * A landscape in the theme's colors: a low sun, three ridges fading into the distance and a trail winding up the
 * nearest one. Recolors with the palette, so it always belongs.
 */
@Composable
private fun TrailHero(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val sky = scheme.primaryContainer
    val sun = scheme.tertiaryContainer
    val glow = scheme.surfaceBright
    val far = scheme.secondary.copy(alpha = 0.35f)
    val mid = scheme.secondary.copy(alpha = 0.6f)
    val near = scheme.primary
    val trail = scheme.onPrimary.copy(alpha = 0.85f)
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(32.dp))
    ) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(sky, sky.copy(alpha = 0.55f))))
        drawCircle(glow.copy(alpha = 0.45f), radius = h * 0.2f, center = Offset(w * 0.74f, h * 0.34f))
        drawCircle(sun, radius = h * 0.13f, center = Offset(w * 0.74f, h * 0.34f))
        fun ridge(color: Color, points: List<Pair<Float, Float>>) {
            val path = Path().apply {
                moveTo(0f, h)
                points.forEach { (x, y) -> lineTo(w * x, h * y) }
                lineTo(w, h)
                close()
            }
            drawPath(path, color)
        }
        ridge(far, listOf(0f to 0.55f, 0.18f to 0.38f, 0.34f to 0.5f, 0.52f to 0.3f, 0.7f to 0.48f, 0.86f to 0.36f, 1f to 0.46f))
        ridge(mid, listOf(0f to 0.7f, 0.22f to 0.52f, 0.4f to 0.66f, 0.6f to 0.5f, 0.8f to 0.64f, 1f to 0.56f))
        ridge(near, listOf(0f to 0.86f, 0.3f to 0.68f, 0.55f to 0.8f, 0.78f to 0.7f, 1f to 0.82f))
        val path = Path().apply {
            moveTo(w * 0.18f, h)
            cubicTo(w * 0.32f, h * 0.94f, w * 0.2f, h * 0.86f, w * 0.34f, h * 0.8f)
            cubicTo(w * 0.42f, h * 0.76f, w * 0.38f, h * 0.72f, w * 0.3f, h * 0.69f)
        }
        drawPath(
            path,
            trail,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)))
        )
    }
}

@Composable
private fun UnitPreviewValue(
    value: String,
    unit: String,
    label: String,
    metric: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.padding(vertical = TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = metric,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "unitPreview"
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A following page (the disclaimer, a missing sensor): its icon in a cookie well, the title, the text in a card. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    page: OnboardingPage,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingFrame(
        nextText = page.nextButtonText ?: stringResource(R.string.next),
        onNext = onNext,
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialShapes.Cookie6Sided.toShape(),
            modifier = Modifier
                .padding(top = TrailSenseSpacing.xl)
                .size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("page_image")
                )
            }
        }
        Text(
            text = page.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.xl)
                .testTag("page_name"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.lg),
            shape = RoundedCornerShape(TrailSenseShape.groupOuter),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            OnboardingPageContents(
                contents = page.contents,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.xl)
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
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
