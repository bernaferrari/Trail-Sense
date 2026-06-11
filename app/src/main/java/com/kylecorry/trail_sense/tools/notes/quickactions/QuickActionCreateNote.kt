package com.kylecorry.trail_sense.tools.notes.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class QuickActionCreateNote(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_notes)
    }

    override fun onClick() {
        super.onClick()
        host.findNavController().navigateWithAnimation(R.id.fragmentToolNotesCreate)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigateWithAnimation(R.id.fragmentToolNotes)
        return true
    }

}
