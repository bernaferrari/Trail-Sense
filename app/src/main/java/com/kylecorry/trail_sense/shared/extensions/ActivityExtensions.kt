package com.kylecorry.trail_sense.shared.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.kylecorry.trail_sense.main.MainActivity

fun AppCompatActivity.findNavController(): NavController? {
    return (this as? MainActivity)?.getRootNavController()
}
