package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

internal fun Modifier.inspectChart(
    label: String,
    selected: String?,
    previousLabel: String,
    nextLabel: String,
    move: (Int) -> Boolean
): Modifier = semantics {
    contentDescription = label
    selected?.let { stateDescription = it }
    customActions = listOf(
        CustomAccessibilityAction(previousLabel) { move(-1) },
        CustomAccessibilityAction(nextLabel) { move(1) }
    )
}.onKeyEvent {
    if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
    when (it.key) {
        Key.DirectionLeft -> move(-1)
        Key.DirectionRight -> move(1)
        else -> false
    }
}.focusable()
