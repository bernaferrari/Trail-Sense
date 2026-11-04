package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent destination shell for app bars, floating actions, safe scaffold insets, and centered
 * tablet content. Screens remain responsible for their own scrolling container.
 */
@Composable
fun TrailSenseScreenScaffold(
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp = TrailSenseSize.maximumContentWidth,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.surface
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
            ) {
                content()
            }
        }
    }
}

/**
 * The common canvas for list, form, and article destinations. It keeps phone padding generous and
 * prevents tablet layouts from stretching into unreadable full-width columns.
 */
@Composable
fun TrailSenseScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = TrailSenseSpacing.screen,
        vertical = TrailSenseSpacing.lg
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl),
                content = content
            )
        }
    }
}

/** A divider-free family of tonal rows with QuietGuard's 24/6 dp expressive silhouette. */
@Composable
fun TrailSenseGroup(
    count: Int,
    modifier: Modifier = Modifier,
    item: @Composable ColumnScope.(index: Int, position: TrailSenseGroupPosition) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        repeat(count) { index ->
            item(index, TrailSenseGroupPosition.at(index, count))
        }
    }
}

/**
 * A large status surface for the single most important value or state on a tool screen. Secondary
 * information belongs below it, so the view always has one clear visual anchor.
 */
@Composable
fun TrailSenseHeroCard(
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    title: String,
    supportingText: String? = null,
    icon: Painter? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    action: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(TrailSenseSize.leadingIconContainer),
                        shape = MaterialTheme.shapes.large,
                        color = contentColor.copy(alpha = 0.12f),
                        contentColor = contentColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.size(TrailSenseSize.leadingIcon)
                            )
                        }
                    }
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor.copy(alpha = 0.82f)
                        )
                    }
                }
                if (action != null) {
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    action()
                }
            }
            content?.invoke(this)
        }
    }
}

/** Stable, tabular typography for live outdoor readings, timers, and measurements. */
@Composable
fun TrailSenseMetric(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    unit: String? = null,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    textAlign: TextAlign = TextAlign.Start
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = style.copy(fontFeatureSettings = "tnum"),
                textAlign = textAlign
            )
            if (unit != null) {
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Icon action with a mandatory spoken label and a 48 dp target. */
@Composable
fun TrailSenseIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(TrailSenseSize.minimumTouchTarget)
            .heightIn(min = TrailSenseSize.minimumTouchTarget),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(TrailSenseSize.leadingIcon)
        )
    }
}
