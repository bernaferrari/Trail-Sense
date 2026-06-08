package com.kylecorry.trail_sense.main

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.size
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

    override fun getMaxItemCount(): Int {
        return MAX_ITEM_COUNT
    }

    fun disable() {
        for (i in 0 until menu.size) {
            menu[i].isEnabled = false
        }
    }

    fun enable() {
        for (i in 0 until menu.size) {
            menu[i].isEnabled = true
        }
    }

    fun setItemLongClickListener(listener: (itemId: Int) -> Boolean) {
        for (i in 0 until menu.size) {
            val item = menu[i]
            findChildById(this, item.itemId)?.setOnLongClickListener {
                listener(item.itemId)
            }
        }
    }

    private fun findChildById(view: View, id: Int): View? {
        if (view.id == id) {
            return view
        }

        if (view !is ViewGroup) {
            return null
        }

        return view.children.firstNotNullOfOrNull {
            findChildById(it, id)
        }
    }

    companion object {
        const val MAX_ITEM_COUNT = 8
    }

}
