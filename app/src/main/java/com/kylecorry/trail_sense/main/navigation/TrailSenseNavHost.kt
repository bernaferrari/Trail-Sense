package com.kylecorry.trail_sense.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kylecorry.trail_sense.tools.tools.ui.ToolsScreen

@Composable
fun TrailSenseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "tools"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("tools") {
        }
    }
}
