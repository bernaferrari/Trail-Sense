package com.kylecorry.trail_sense.shared.quickactions

import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class ComposeQuickActionButtonView : QuickActionButtonView {

    override var isVisible by mutableStateOf(false)
    override var contentDescription by mutableStateOf<CharSequence?>(null)
    var isClickable by mutableStateOf(true)
    private var iconVector by mutableStateOf<ImageVector?>(null)
    private var isOn by mutableStateOf(false)
    private var onClick: (() -> Unit)? = null
    private var onLongClick: (() -> Boolean)? = null
    private var onPress by mutableStateOf<((pressed: Boolean) -> Unit)?>(null)

    override fun setIcon(icon: ImageVector) {
        iconVector = icon
    }

    override fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    override fun setOnClickListener(listener: (() -> Unit)?) {
        onClick = listener
    }

    override fun setOnLongClickListener(listener: (() -> Boolean)?) {
        onLongClick = listener
    }

    override fun setOnPressListener(listener: ((pressed: Boolean) -> Unit)?) {
        onPress = listener
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Content(
        modifier: Modifier = Modifier,
        offContainerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
        offContentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
    ) {
        val icon = iconVector
        if (!isVisible || icon == null) {
            Box(modifier = modifier.size(48.dp))
            return
        }

        // On: the circle rounds into a square and fills with the accent container, the expressive toggle shape.
        val container by animateColorAsState(
            if (isOn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                offContainerColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh }
            },
            MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "quickActionContainer"
        )
        val tint by animateColorAsState(
            if (isOn) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                offContentColor.takeOrElse { MaterialTheme.colorScheme.onSurface }
            },
            MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "quickActionTint"
        )
        val corner by animateIntAsState(
            if (isOn) 30 else 50,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "quickActionCorner"
        )

        Surface(
            modifier = modifier.size(52.dp),
            shape = RoundedCornerShape(percent = corner),
            color = container
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (val press = onPress) {
                            null -> Modifier.combinedClickable(
                                enabled = isClickable,
                                onClick = { onClick?.invoke() },
                                onLongClick = { onLongClick?.invoke() }
                            )
                            else -> Modifier.pointerInput(press, isClickable) {
                                if (!isClickable) return@pointerInput
                                detectTapGestures(onPress = {
                                    press(true)
                                    tryAwaitRelease()
                                    press(false)
                                })
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription?.toString(),
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
