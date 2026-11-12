package com.kylecorry.trail_sense.onboarding

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.serialization.Serializable

/** One step of first run: 0 is the unit system, the rest are [OnboardingPages] in order. */
@Serializable
data class OnboardingStep(val index: Int) : NavKey

/**
 * First run, shown by MainActivity in place of the app until it is finished. Each page is a Navigation 3 entry, so
 * Next pushes the following page, Back pops to the previous one (and leaves the app from the first), and the pages
 * survive a configuration change or process death. Finishing the last page saves the units and the completed flag,
 * then calls [onFinished].
 */
@Composable
fun OnboardingFlow(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pages = remember { OnboardingPages.getPages(context) }
    val backStack = rememberNavBackStack(OnboardingStep(0))
    var useMetricUnits by rememberSaveable { mutableStateOf(Resources.isMetricPreferred(context)) }

    fun next(from: Int) {
        // Only the page on screen moves on, so a double tap on Next can't skip a page or finish twice
        if (backStack.lastOrNull() != OnboardingStep(from)) return
        if (from >= pages.size) {
            Onboarding.complete(context)
            onFinished()
        } else {
            backStack.add(OnboardingStep(from + 1))
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = { key ->
            val step = (key as OnboardingStep).index
            NavEntry(key) {
                if (step == 0) {
                    UnitSystemOnboardingScreen(
                        useMetric = useMetricUnits,
                        onUnitSystemChanged = { useMetricUnits = it },
                        onNext = {
                            Onboarding.saveUnitSystem(context, useMetricUnits)
                            next(0)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // The disclaimer is always a page; a step past the end (a stack restored after the pages
                    // changed, such as a compass now found) shows the last one
                    OnboardingScreen(
                        page = pages.getOrNull(step - 1) ?: pages.last(),
                        onNext = { next(step) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    )
}

object Onboarding {

    fun isCompleted(context: Context): Boolean {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        return prefs.getBoolean(PreferenceKeys.onboardingCompleted) == true
    }

    fun complete(context: Context) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.putBoolean(PreferenceKeys.mainDisclaimerShownKey, true)
        prefs.putBoolean(PreferenceKeys.onboardingCompleted, true)
    }

    fun saveUnitSystem(context: Context, useMetric: Boolean) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        prefs.putString(PreferenceKeys.distanceUnits, if (useMetric) "meters" else "feet_miles")
        prefs.putString(PreferenceKeys.pressureUnits, if (useMetric) "hpa" else "in")
        prefs.putString(PreferenceKeys.temperatureUnits, if (useMetric) "c" else "f")
        prefs.putString(PreferenceKeys.weightUnits, if (useMetric) "kg" else "lbs")
    }
}
