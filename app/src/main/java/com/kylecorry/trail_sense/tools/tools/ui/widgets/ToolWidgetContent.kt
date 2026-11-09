package com.kylecorry.trail_sense.tools.tools.ui.widgets

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.graphics.Bitmap

sealed interface ToolWidgetContent {
    val clickAction: WidgetClickAction?

    data class Simple(
        val title: CharSequence?,
        val subtitle: CharSequence?,
        val icon: ImageVector? = null,
        val iconBitmap: Bitmap? = null,
        val showTitle: Boolean = true,
        override val clickAction: WidgetClickAction? = null
    ) : ToolWidgetContent

    data class Title(
        val title: CharSequence?,
        override val clickAction: WidgetClickAction? = null
    ) : ToolWidgetContent

    /**
     * A chart: [drawing] (drawn live in the app and into the home-screen widget's image) or a ready [bitmap] (the
     * map). [emptyText] stands in for the chart when there is nothing to draw yet.
     */
    data class Chart(
        val title: CharSequence? = null,
        val drawing: WidgetChart? = null,
        val bitmap: Bitmap? = null,
        val emptyText: CharSequence? = null,
        override val clickAction: WidgetClickAction? = null
    ) : ToolWidgetContent

    data class List(
        val title: CharSequence,
        val emptyText: CharSequence,
        val items: kotlin.collections.List<ListItem>,
        override val clickAction: WidgetClickAction? = null
    ) : ToolWidgetContent
}

data class ListItem(
    val title: CharSequence,
    val description: CharSequence? = null,
    val icon: ImageVector? = null,
    val iconTint: Int? = null,
    val iconBackground: Int? = null,
    val clickAction: WidgetClickAction? = null
)

sealed interface WidgetClickAction {
    data class StartActivity(val intent: Intent) : WidgetClickAction
    data class SendBroadcast(val intent: Intent) : WidgetClickAction
}
