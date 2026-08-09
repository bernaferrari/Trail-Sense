package com.kylecorry.trail_sense.onboarding

import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPage(
    val title: String,
    val contents: CharSequence,
    val image: ImageVector,
    val nextButtonText: String? = null,
)
