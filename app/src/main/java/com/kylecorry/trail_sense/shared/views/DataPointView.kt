package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R

class DataPointView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var icon by mutableStateOf<Int?>(null)
    private var titleText by mutableStateOf("")
    private var descriptionText by mutableStateOf("")
    private var showDescriptionState by mutableStateOf(true)
    private var descriptionClickListener by mutableStateOf<(() -> Unit)?>(null)
    private var contentGravityState by mutableIntStateOf(Gravity.START)
    private var tint by mutableIntStateOf(0)
    private val contentView = ComposeView(context)

    var title: String
        get() = titleText
        set(value) {
            titleText = value
        }

    var description: String
        get() = descriptionText
        set(value) {
            descriptionText = value
        }

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DataPointView, 0, 0)
        tint = a.getColor(
            R.styleable.DataPointView_dataPointTint,
            0
        )
        val initialIcon = a.getResourceId(R.styleable.DataPointView_dataPointIcon, -1)
        setImageResource(if (initialIcon == -1) null else initialIcon)
        title = a.getString(R.styleable.DataPointView_dataPointText) ?: ""
        description = a.getString(R.styleable.DataPointView_dataPointDescription) ?: ""
        contentGravityState = a.getInt(R.styleable.DataPointView_android_gravity, Gravity.START)
        a.recycle()

        addView(
            contentView.apply {
                setContent {
                    TrailSenseTheme {
                        DataPointContent(
                            title = titleText,
                            description = descriptionText,
                            icon = icon,
                            tint = tint.takeUnless { it == 0 }?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.onSurface,
                            showDescription = showDescriptionState,
                            gravity = contentGravityState,
                            onDescriptionClick = descriptionClickListener
                        )
                    }
                }
            },
            createContentLayoutParams(contentGravityState)
        )
    }


    fun setImageResource(@DrawableRes res: Int?) {
        icon = res
    }

    fun setContentGravity(gravity: Int) {
        contentGravityState = gravity
        contentView.layoutParams = createContentLayoutParams(gravity)
    }

    fun setShowDescription(showDescription: Boolean) {
        showDescriptionState = showDescription
    }

    fun setOnDescriptionClickListener(listener: (() -> Unit)?) {
        descriptionClickListener = listener
    }

}

private fun createContentLayoutParams(gravity: Int): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).also {
        it.gravity = gravity
    }
}

@Composable
fun DataPointContent(
    title: String,
    description: String,
    @DrawableRes icon: Int?,
    tint: Color,
    showDescription: Boolean,
    gravity: Int,
    onDescriptionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .wrapContentSize(gravity.toAlignment())
            .testTag("data_point"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint.copy(alpha = 0.75f),
                modifier = Modifier.testTag("data_point_image")
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("data_point_title")
            )
            if (showDescription) {
                val descriptionModifier = if (onDescriptionClick != null) {
                    Modifier.clickable { onDescriptionClick() }
                } else {
                    Modifier
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = descriptionModifier.testTag("data_point_desc")
                )
            }
        }
    }
}

private fun Int.toAlignment(): Alignment {
    return when {
        this and Gravity.CENTER_HORIZONTAL == Gravity.CENTER_HORIZONTAL -> Alignment.Center
        this and Gravity.RIGHT == Gravity.RIGHT || this and Gravity.END == Gravity.END -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
}
