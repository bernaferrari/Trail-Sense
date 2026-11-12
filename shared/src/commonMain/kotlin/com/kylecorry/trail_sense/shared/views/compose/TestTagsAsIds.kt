package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.Modifier

/**
 * Exposes the test tags under this node as platform ids, so UI tests can find them. Separate windows (such as
 * dialogs) don't inherit this from the app root, so they apply it themselves.
 */
expect fun Modifier.testTagsAsIds(): Modifier
