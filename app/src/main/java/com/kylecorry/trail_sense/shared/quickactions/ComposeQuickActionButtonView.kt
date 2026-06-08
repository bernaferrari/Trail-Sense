package com.kylecorry.trail_sense.shared.quickactions

import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

class ComposeQuickActionButtonView : QuickActionButtonView {

    override var isVisible by mutableStateOf(false)
    override var contentDescription by mutableStateOf<CharSequence?>(null)
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

        val tint = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Box(
            modifier = modifier
                .size(48.dp)
                .pointerInteropFilter { onTouch?.invoke(it) ?: false }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
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
