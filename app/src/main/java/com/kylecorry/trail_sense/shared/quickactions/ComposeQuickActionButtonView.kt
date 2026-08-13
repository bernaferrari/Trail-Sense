package com.kylecorry.trail_sense.shared.quickactions

import android.view.MotionEvent
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class ComposeQuickActionButtonView : QuickActionButtonView {

    override var isVisible by mutableStateOf(false)
    override var contentDescription by mutableStateOf<CharSequence?>(null)
    var isClickable by mutableStateOf(true)
    private var iconRes by mutableIntStateOf(0)
    private var isOn by mutableStateOf(false)
    private var onClick: (() -> Unit)? = null
    private var onLongClick: (() -> Boolean)? = null
    private var onTouch: ((event: MotionEvent) -> Boolean)? = null

    override fun setIcon(@DrawableRes icon: Int) {
        iconRes = icon
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

    override fun setOnTouchListener(listener: ((event: MotionEvent) -> Boolean)?) {
        onTouch = listener
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible || iconRes == 0) {
            Box(modifier = modifier.size(48.dp))
            return
        }

        val container = if (isOn) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val tint = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

        Surface(
            modifier = modifier.size(48.dp),
            shape = CircleShape,
            color = container
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { isClickable && (onTouch?.invoke(it) ?: false) }
                    .combinedClickable(
                        enabled = isClickable,
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription?.toString(),
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
