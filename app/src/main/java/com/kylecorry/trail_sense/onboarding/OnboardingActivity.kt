package com.kylecorry.trail_sense.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.theme.TrailSenseAppTheme
import com.kylecorry.trail_sense.shared.views.TrailSenseColorScheme
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class OnboardingActivity : ComponentActivity() {

    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }
    private val pages by lazy { OnboardingPages.getPages(this) }

    private var pageIdx by mutableIntStateOf(0)
    private var useMetricUnits by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        useMetricUnits = Resources.isMetricPreferred(this)

        setContent {
            TrailSenseAppTheme(
                darkTheme = isSystemInDarkTheme(),
                isAmoled = false,
                useDynamicColors = false,
                fallbackColorScheme = TrailSenseColorScheme.Teal
            ) {
                if (pageIdx == 0) {
                    UnitSystemOnboardingScreen(
                        useMetric = useMetricUnits,
                        onUnitSystemChanged = { useMetricUnits = it },
                        onNext = {
                            saveUnitSystem(useMetricUnits)
                            load(1)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val page = pages.getOrNull(pageIdx - 1) ?: return@TrailSenseAppTheme
                    OnboardingScreen(
                        page = page,
                        onNext = { load(pageIdx + 1) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        setupBackPressed()
        load(pageIdx)
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this) {
            if (pageIdx == 0) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            } else {
                load(pageIdx - 1)
            }
        }
    }

    private fun navigateToApp() {
        cache.putBoolean(getString(R.string.pref_main_disclaimer_shown_key), true)
        cache.putBoolean(getString(R.string.pref_onboarding_completed), true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveUnitSystem(useMetric: Boolean) {
        cache.putString(getString(R.string.pref_distance_units), if (useMetric) "meters" else "feet_miles")
        cache.putString(getString(R.string.pref_pressure_units), if (useMetric) "hpa" else "in")
        cache.putString(getString(R.string.pref_temperature_units), if (useMetric) "c" else "f")
        cache.putString(getString(R.string.pref_weight_units), if (useMetric) "kg" else "lbs")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pageIdx = savedInstanceState.getInt("page", 0)
        if (pageIdx > pages.size || pageIdx < 0) {
            pageIdx = 0
        }
        load(pageIdx)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", pageIdx)
    }

    private fun load(page: Int) {
        pageIdx = page

        if (page > pages.size) {
            navigateToApp()
        }
    }
}
