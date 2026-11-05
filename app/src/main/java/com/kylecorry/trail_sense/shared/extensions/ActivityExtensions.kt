package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.ComponentActivity
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.main.MainActivity

fun ComponentActivity.findNavigator(): TrailSenseNavigator? {
    return (this as? MainActivity)?.navigator
}
