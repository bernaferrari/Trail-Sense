package com.kylecorry.trail_sense.shared.views.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.andromeda.fragments.ReactiveAndromedaFragment
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

abstract class TrailSenseReactiveComposeFragment : ReactiveAndromedaFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        beforeCreateContent()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    FragmentContent()
                }
            }
        }
    }

    @Composable
    protected abstract fun FragmentContent()

    protected open fun beforeCreateContent() {
    }
}
