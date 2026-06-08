package com.kylecorry.trail_sense.shared.views.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.fragments.ReactiveAndromedaFragment

abstract class TrailSenseReactiveComposeFragment : ReactiveAndromedaFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        beforeCreateContent()
        return TrailSenseComposeRootView(requireContext()) {
            FragmentContent()
        }
    }

    @Composable
    protected abstract fun FragmentContent()

    protected open fun beforeCreateContent() {
    }
}
