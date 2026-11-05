package com.kylecorry.trail_sense.tools.tools.quickactions

import androidx.compose.runtime.mutableStateListOf
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MainActivityQuickActionBinder(
    private val host: TrailSenseDestinationHost,
    private val state: MainActivityQuickActionState
) : IQuickActionBinder {

    private val prefs by lazy { UserPreferences(host.requireContext()) }

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

        val appNavigator = host.findNavigator()
        val tools = Tools.getTools(host.requireContext())
        val currentDestination = appNavigator.currentDestination?.id ?: 0
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
            val action = factory.create(it, createButton(true), host)
            action.bind(host.lifecycleOwner)
        }

        selected.forEach {
            val action = factory.create(it, createButton(false), host)
            action.bind(host.lifecycleOwner)
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
