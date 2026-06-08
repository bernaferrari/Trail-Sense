package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class PhotoMapIconButton(
    context: Context,
    icon: Int
) {

    private var iconRes by mutableIntStateOf(icon)
    private var isOn by mutableStateOf(false)
    private var buttonVisible by mutableStateOf(true)
    private var alphaState by mutableStateOf(1f)
    private var clickListener: View.OnClickListener? = null

    val view = ComposeView(context).apply {
        setContent {
            TrailSenseTheme {
                Content()
            }
        }
    }

    var id: Int
        get() = view.id
        set(value) {
            view.id = value
        }

    var alpha: Float
        get() = alphaState
        set(value) {
            alphaState = value
            view.alpha = value
        }

    var isClickable: Boolean
        get() = view.isClickable
        set(value) {
            view.isClickable = value
        }

    var isFocusable: Boolean
        get() = view.isFocusable
        set(value) {
            view.isFocusable = value
        }

    var isVisible: Boolean
        get() = buttonVisible
        set(value) {
            buttonVisible = value
            view.visibility = if (value) View.VISIBLE else View.GONE
        }

    fun setOnClickListener(listener: View.OnClickListener?) {
        clickListener = listener
        view.setOnClickListener(listener)
    }

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!buttonVisible) {
            return
        }
        val background = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val foreground = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            modifier = modifier
                .size(40.dp)
                .alpha(alphaState)
                .clickable(enabled = isClickable) {
                    clickListener?.onClick(view)
                },
            shape = CircleShape,
            color = background,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
