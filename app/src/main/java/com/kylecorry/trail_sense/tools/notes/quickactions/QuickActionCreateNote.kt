package com.kylecorry.trail_sense.tools.notes.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionCreateNote(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolNotes)
    }

    override fun onClick() {
        super.onClick()
        host.findNavigator().navigate(R.id.fragmentToolNotesCreate)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().navigate(R.id.fragmentToolNotes)
        return true
    }

}
