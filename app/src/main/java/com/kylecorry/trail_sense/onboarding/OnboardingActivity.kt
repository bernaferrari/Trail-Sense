package com.kylecorry.trail_sense.onboarding

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme


class OnboardingActivity : AndromedaActivity() {

    private val cache by lazy { PreferencesSubsystem.getInstance(this).preferences }
    private val markdown by lazy { DependencyRegistry.get<MarkdownService>() }
    private val pages by lazy { OnboardingPages.getPages(this) }

    private lateinit var onboardingViews: OnboardingViews

    private var pageIdx = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        setColorTheme(ColorTheme.System, true)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onboardingViews = createOnboardingViews(this)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    TrailSenseTheme {
                        OnboardingHost(onboardingViews)
                    }
                }
            }
        )

        bindLayoutInsets()

        load(pageIdx)

        onboardingViews.nextButton.setOnClickListener {
            load(pageIdx + 1)
        }

        setupBackPressed()

    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this) {
            if (pageIdx == 0) {
                // Allow default back behavior
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            } else {
                load(pageIdx - 1)
            }
        }
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(onboardingViews.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun navigateToApp() {
        // Consider the disclaimer as shown
        cache.putBoolean(getString(R.string.pref_main_disclaimer_shown_key), true)
        cache.putBoolean(getString(R.string.pref_onboarding_completed), true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        pageIdx = savedInstanceState.getInt("page", 0)
        if (pageIdx >= pages.size || pageIdx < 0) {
            pageIdx = 0
        }
        load(pageIdx)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", pageIdx)
    }

    private fun load(page: Int) {
        onboardingViews.pageSettings.removeAllViews()

        pageIdx = page

        if (page >= pages.size) {
            navigateToApp()
        } else {
            val pageContents = pages[page]
            onboardingViews.pageName.title.text = pageContents.title
            onboardingViews.pageImage.setImageResource(pageContents.image)
            onboardingViews.pageImage.imageTintList =
                ColorStateList.valueOf(Resources.androidTextColorPrimary(this))
            onboardingViews.nextButton.text = pageContents.nextButtonText ?: getString(R.string.next)
            onboardingViews.pageContents.movementMethod = LinkMovementMethodCompat.getInstance()
            if (pageContents.contents is String) {
                markdown.setMarkdown(onboardingViews.pageContents, pageContents.contents)
            } else {
                onboardingViews.pageContents.text = pageContents.contents
            }
        }
    }

}

private data class OnboardingViews(
    val root: View,
    val pageName: Toolbar,
    val pageImage: ImageView,
    val pageContents: TextView,
    val pageSettings: LinearLayout,
    val nextButton: Button
)

@Composable
private fun OnboardingHost(views: OnboardingViews) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { views.root }
    )
}

private fun createOnboardingViews(activity: OnboardingActivity): OnboardingViews {
    val root = LinearLayout(activity).apply {
        id = R.id.coordinator
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val scrollView = ScrollView(activity).apply {
        clipToPadding = false
        setPadding(0, 0, 0, Resources.dp(activity, 16f).toInt())
    }
    root.addView(
        scrollView,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply {
            bottomMargin = Resources.dp(activity, 16f).toInt()
        }
    )

    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            Resources.dp(activity, 16f).toInt(),
            0,
            Resources.dp(activity, 16f).toInt(),
            0
        )
    }
    scrollView.addView(
        content,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val pageName = Toolbar(activity, null).apply {
        id = R.id.page_name
        subtitle.visibility = View.GONE
    }
    content.addView(
        pageName,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val pageImage = ImageView(activity).apply {
        id = R.id.page_image
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    content.addView(
        pageImage,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Resources.dp(activity, 100f).toInt()
        )
    )

    val pageContents = TextView(activity).apply {
        id = R.id.page_contents
        setLineSpacing(0f, 1.5f)
    }
    content.addView(
        pageContents,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = Resources.dp(activity, 16f).toInt()
        }
    )

    val pageSettings = LinearLayout(activity).apply {
        id = R.id.page_settings
        orientation = LinearLayout.VERTICAL
    }
    content.addView(
        pageSettings,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = Resources.dp(activity, 16f).toInt()
            bottomMargin = Resources.dp(activity, 16f).toInt()
        }
    )

    val nextButton = Button(activity).apply {
        id = R.id.next_button
        text = activity.getString(R.string.continue_button)
    }
    root.addView(
        nextButton,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = activity.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    return OnboardingViews(root, pageName, pageImage, pageContents, pageSettings, nextButton)
}
