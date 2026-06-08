package com.kylecorry.trail_sense.tools.tools.quickactions

import androidx.compose.runtime.mutableStateListOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivityQuickActionBinder(
    private val fragment: Fragment,
    private val state: MainActivityQuickActionState
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

    private fun createButton(recommended: Boolean): ComposeQuickActionButtonView {
        val button = ComposeQuickActionButtonView()
        if (recommended) {
            state.recommended.add(button)
        } else {
            state.selected.add(button)
        }

        return button
    }

    override fun bind() {
        state.clear()

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
            val action = factory.create(it, createButton(true), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }

        selected.forEach {
            val action = factory.create(it, createButton(false), fragment)
            action.bind(fragment.viewLifecycleOwner)
        }
    }
}

class MainActivityQuickActionState {
    val recommended = mutableStateListOf<ComposeQuickActionButtonView>()
    val selected = mutableStateListOf<ComposeQuickActionButtonView>()

    fun clear() {
        recommended.clear()
        selected.clear()
    }
}
