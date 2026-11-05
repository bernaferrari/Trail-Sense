package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects

/**
 * The family's share sheet (the same titles, icons and order as Share.share) through [ScreenEffects], so a screen can
 * offer it without a destination host. Returns the chosen action, or null when the sheet was dismissed.
 */
internal suspend fun chooseShareAction(
    context: Context,
    effects: ScreenEffects,
    title: String,
    actions: List<ShareAction>,
    subtitle: String? = null
): ShareAction? {
    val titles = mapOf(
        ShareAction.Copy to context.getString(android.R.string.copy),
        ShareAction.QR to context.getString(R.string.qr_code),
        ShareAction.Send to context.getString(R.string.share_action_send),
        ShareAction.Maps to context.getString(R.string.maps),
        ShareAction.File to context.getString(R.string.file),
        ShareAction.Navigate to context.getString(R.string.navigate),
        ShareAction.Open to context.getString(R.string.open)
    )

    val icons = mapOf(
        ShareAction.Copy to TrailIcons.Copy,
        ShareAction.QR to TrailIcons.QrCode,
        ShareAction.Send to TrailIcons.Send,
        ShareAction.Maps to TrailIcons.Maps,
        ShareAction.File to TrailIcons.File,
        ShareAction.Navigate to TrailIcons.Beacon,
        ShareAction.Open to TrailIcons.Open
    )

    val sorted = actions.sortedBy { it.ordinal }
    val items = sorted.map { ActionItem(titles[it] ?: "", icons[it] ?: TrailIcons.Send) {} }
    val chosen = effects.chooseAction(title, subtitle, items) ?: return null
    return sorted.getOrNull(items.indexOfFirst { it === chosen })
}
