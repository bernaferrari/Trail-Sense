package com.kylecorry.trail_sense.shared.grouping.lists

import com.kylecorry.trail_sense.shared.grouping.Groupable

fun <T : Groupable> GroupListManager<T>.bind(view: com.kylecorry.trail_sense.shared.views.SearchView) {
    view.setOnSearchListener {
        this.search(it)
    }
}
