package com.kylecorry.trail_sense.tools.tools.quickactions

import android.widget.ImageButton
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.ImageButtonQuickActionView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivityQuickActionBinder(
    private val fragment: Fragment,
    private val recommendedQuickActions: FlexboxLayout,
    private val quickActions: FlexboxLayout
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private fun createButton(recommended: Boolean): ImageButton {
        val size = Resources.dp(fragment.requireContext(), 40f).toInt()
        val margins = Resources.dp(fragment.requireContext(), 8f).toInt()
        val button = ImageButton(fragment.requireContext())
        button.layoutParams = FlexboxLayout.LayoutParams(size, size).apply {
            setMargins(margins)
        }
        button.background =
            Resources.drawable(fragment.requireContext(), R.drawable.rounded_rectangle)
        button.elevation = 2f

        if (recommended) {
            recommendedQuickActions.addView(button)
        } else {
            quickActions.addView(button)
        }

        CustomUiUtils.setButtonState(button, false)

        return button
    }

    override fun bind() {
        quickActions.removeAllViews()
        recommendedQuickActions.removeAllViews()

        val selected = prefs.toolQuickActions.sorted()

        val navController = fragment.findNavController()
        val tools = Tools.getTools(fragment.requireContext())
        val currentDestination = navController.currentDestination?.id ?: 0
        val activeTools = tools
            .filter { it.isOpen(currentDestination) || it.settingsNavAction == currentDestination }

        val alwaysRecommended = listOf(
            Tools.QUICK_ACTION_USER_GUIDE,
            Tools.QUICK_ACTION_SETTINGS
        )

        val activeToolQuickActions = activeTools
            .flatMap { it.quickActions }
            .map { it.id }
            .filterNot { it in alwaysRecommended }
            .sorted()

        val recommended = activeToolQuickActions + alwaysRecommended

        val factory = QuickActionFactory()

        recommended.distinct().forEach {
            val action = factory.create(it, ImageButtonQuickActionView(createButton(true)), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }

        selected.forEach {
            val action = factory.create(it, ImageButtonQuickActionView(createButton(false)), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }
    }
}
