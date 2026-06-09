package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.ComponentActivity
import androidx.navigation.NavController
import com.kylecorry.trail_sense.main.MainActivity

fun ComponentActivity.findNavController(): NavController? {
    return (this as? MainActivity)?.getRootNavController()
}
