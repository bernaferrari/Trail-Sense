@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tools.ui.widgets

import androidx.compose.ui.graphics.vector.ImageVector
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kylecorry.trail_sense.shared.icons.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme

@Composable
fun ToolWidgetGlanceContent(
    content: ToolWidgetContent?,
    theme: WidgetTheme,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val colors = WidgetColors.fromTheme(theme)
    val backgroundModifier = when (theme) {
        WidgetTheme.System -> modifier.background(colors.background)
        WidgetTheme.TransparentBlack, WidgetTheme.TransparentWhite -> modifier
    }

    Box(
        modifier = backgroundModifier
            .fillMaxSize()
            .padding(8.dp)
            .thenClick(content?.clickAction),
        contentAlignment = Alignment.Center
    ) {
        when (content) {
            is ToolWidgetContent.Chart -> ChartContent(content, colors)
            is ToolWidgetContent.List -> ListContent(content, colors)
            is ToolWidgetContent.Simple -> SimpleContent(content, colors)
            is ToolWidgetContent.Title -> TitleContent(content.title?.toString().orEmpty(), colors)
            null -> SimpleContent(
                ToolWidgetContent.Simple(
                    title = context.getString(R.string.app_name),
                    subtitle = "",
                    icon = WidgetIcons.launcher(context)
                ),
                colors
            )
        }
    }
}

@Composable
private fun SimpleContent(content: ToolWidgetContent.Simple, colors: WidgetColors) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetIcon(
            icon = content.icon,
            bitmap = content.iconBitmap,
            colors = colors
        )
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (content.showTitle) {
                Text(
                    text = content.title?.toString().orEmpty(),
                    style = TextStyle(
                        color = colors.primaryText,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = content.subtitle?.toString().orEmpty(),
                style = TextStyle(color = colors.secondaryText),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun TitleContent(title: String, colors: WidgetColors) {
    Text(
        text = title,
        style = TextStyle(
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        maxLines = 2
    )
}

@Composable
private fun ChartContent(content: ToolWidgetContent.Chart, colors: WidgetColors) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!content.title.isNullOrBlank()) {
            Text(
                text = content.title.toString(),
                style = TextStyle(color = colors.primaryText, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(4.dp))
        }
        val bitmap = content.bitmap
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = content.title?.toString(),
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ListContent(content: ToolWidgetContent.List, colors: WidgetColors) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = content.title.toString(),
            style = TextStyle(color = colors.primaryText, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(4.dp))
        if (content.items.isEmpty()) {
            Text(
                text = content.emptyText.toString(),
                style = TextStyle(color = colors.secondaryText),
                maxLines = 2
            )
        } else {
            content.items.take(5).forEach { item ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .thenClick(item.clickAction),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WidgetIcon(
                        icon = item.icon,
                        bitmap = null,
                        colors = colors,
                        tint = item.iconTint,
                        background = item.iconBackground
                    )
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = item.title.toString(),
                            style = TextStyle(color = colors.primaryText),
                            maxLines = 1
                        )
                        item.description?.let {
                            Text(
                                text = it.toString(),
                                style = TextStyle(color = colors.secondaryText),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetIcon(
    icon: ImageVector?,
    bitmap: Bitmap?,
    colors: WidgetColors,
    tint: Int? = null,
    background: Int? = null
) {
    val imageProvider = when {
        bitmap != null -> ImageProvider(bitmap)
        icon != null -> ImageProvider(icon.toBitmap(LocalContext.current, 22f))
        else -> null
    }

    Box(
        modifier = GlanceModifier
            .size(32.dp)
            .background(background?.let { ColorProvider(it) } ?: colors.iconBackground)
            .let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    it.cornerRadius(16.dp)
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        imageProvider?.let {
            val colorFilter = if (bitmap == null) {
                androidx.glance.ColorFilter.tint(
                    tint?.let { color -> ColorProvider(color) } ?: colors.icon
                )
            } else {
                null
            }
            Image(
                provider = it,
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
                colorFilter = colorFilter
            )
        }
    }
}

private fun GlanceModifier.thenClick(action: WidgetClickAction?): GlanceModifier {
    return when (val glanceAction = action?.toGlanceAction()) {
        null -> this
        else -> clickable(glanceAction)
    }
}

private fun WidgetClickAction.toGlanceAction(): Action {
    return when (this) {
        is WidgetClickAction.SendBroadcast -> actionSendBroadcast(intent)
        is WidgetClickAction.StartActivity -> actionStartActivity(intent)
    }
}

private data class WidgetColors(
    val background: ColorProvider,
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val icon: ColorProvider,
    val iconBackground: ColorProvider
) {
    companion object {
        fun fromTheme(theme: WidgetTheme): WidgetColors {
            return when (theme) {
                WidgetTheme.TransparentBlack -> WidgetColors(
                    background = ColorProvider(android.graphics.Color.TRANSPARENT),
                    primaryText = ColorProvider(android.graphics.Color.BLACK),
                    secondaryText = ColorProvider(android.graphics.Color.BLACK),
                    icon = ColorProvider(android.graphics.Color.BLACK),
                    iconBackground = ColorProvider(0x22_00_00_00)
                )
                WidgetTheme.TransparentWhite -> WidgetColors(
                    background = ColorProvider(android.graphics.Color.TRANSPARENT),
                    primaryText = ColorProvider(android.graphics.Color.WHITE),
                    secondaryText = ColorProvider(android.graphics.Color.WHITE),
                    icon = ColorProvider(android.graphics.Color.WHITE),
                    iconBackground = ColorProvider(0x33_FF_FF_FF)
                )
                WidgetTheme.System -> WidgetColors(
                    background = ColorProvider(android.graphics.Color.rgb(250, 250, 250)),
                    primaryText = ColorProvider(android.graphics.Color.rgb(32, 33, 36)),
                    secondaryText = ColorProvider(android.graphics.Color.rgb(95, 99, 104)),
                    icon = ColorProvider(android.graphics.Color.rgb(95, 99, 104)),
                    iconBackground = ColorProvider(android.graphics.Color.TRANSPARENT)
                )
            }
        }
    }
}
