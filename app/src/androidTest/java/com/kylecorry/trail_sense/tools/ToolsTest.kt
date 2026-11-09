package com.kylecorry.trail_sense.tools

import com.kylecorry.trail_sense.shared.andromeda_temp.markdown.MarkdownService
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavigator
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolsTest : ToolTestBase(0L) {

    @Test
    fun openWithDynamicColors() {
        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName != null) {
            return
        }

        val prefs = PreferencesSubsystem.getInstance(TestUtils.context).preferences
        prefs.putBoolean(TestUtils.context.getString(R.string.pref_use_dynamic_colors), true)
        prefs.putBoolean(
            TestUtils.context.getString(R.string.pref_use_dynamic_colors_on_compass),
            true
        )

        // Restart the app with Navigation tool (default)
        scenario.close()
        scenario = TestUtils.startWithTool(Tools.NAVIGATION) {
            appNavigator = it.findNavigator()!!
        }

        // Wait for the tool to load
        Thread.sleep(200)

        // Verify navigation tool is open
        isTrue {
            Tools.getTool(TestUtils.context, Tools.NAVIGATION)!!
                .isOpen(appNavigator.currentRoute)
        }
    }

    @Test
    fun catchesToolErrors() {
        // TODO: Figure out how to check this on staging builds (not sure if it will be possible)
        if (AutomationLibrary.packageName != null) {
            return
        }

        scenario.close()

        var calledOriginalHandler = false

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            calledOriginalHandler = true
        }

        // The Compose field guide no longer reads its repo from the registry, so break astronomy instead: its screen
        // reads the markdown service from the registry when it is first composed (and it is on every device)
        val markdown = DependencyRegistry.get<MarkdownService>()
        DependencyRegistry.remove<MarkdownService>()
        scenario = TestUtils.startWithTool(Tools.ASTRONOMY)

        isTrue { calledOriginalHandler }
        hasText("An error occurred")
        hasText(string(R.string.astronomy))

        // Settings
        scrollUntil { click(string(R.string.settings), exact = true) }
        hasText("Units")

        back()
        hasText("An error occurred")

        // View error details
        click(string(R.string.view_error_details), exact = true)
        hasText("Build type")
        clickOk()

        // Other buttons
        hasText(string(R.string.pref_email_title), exact = true)
        hasText(string(R.string.copy_error), exact = true)
        hasText(string(R.string.restart_app), exact = true)

        // Fix the issue and reopen tool
        DependencyRegistry.addSingleton(markdown)
        click(string(R.string.reopen_tool), exact = true)
        not { hasText("An error occurred") }
        hasText(string(R.string.astronomy))
    }
}
