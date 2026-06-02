package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.UserPreferences

class ErrorBannerView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val errorIcon: ImageView
    private val errorText: TextView
    private val errorAction: Button
    private val errorClose: ImageButton

    private val errors: MutableList<UserError> = mutableListOf()

    private val prefs by lazy { UserPreferences(this.context) }

    private var onAction: (() -> Unit)? = null
    private var overallAction: (() -> Unit)? = null

    init {
        setBackgroundColor(context.getColor(R.color.red))
        minHeight = Resources.dp(context, 64f).toInt()
        setPadding(
            Resources.dp(context, 4f).toInt(),
            Resources.dp(context, 4f).toInt(),
            Resources.dp(context, 4f).toInt(),
            Resources.dp(context, 4f).toInt()
        )

        errorIcon = ImageView(context).apply {
            id = R.id.error_icon
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(R.drawable.satellite)
            setColorFilter(Color.BLACK)
        }
        addView(
            errorIcon,
            LayoutParams(
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt()
            ).apply {
                marginStart = Resources.dp(context, 12f).toInt()
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        errorClose = ImageButton(context).apply {
            id = R.id.error_close
            background = AppCompatResources.getDrawable(context, android.R.color.transparent)
            setImageResource(R.drawable.ic_cancel)
            setColorFilter(Color.BLACK)
        }
        addView(
            errorClose,
            LayoutParams(
                Resources.dp(context, 32f).toInt(),
                Resources.dp(context, 32f).toInt()
            ).apply {
                marginEnd = Resources.dp(context, 12f).toInt()
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        errorAction = Button(context).apply {
            id = R.id.error_action
        }
        addView(
            errorAction,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = Resources.dp(context, 12f).toInt()
                endToStart = R.id.error_close
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        errorText = TextView(context).apply {
            id = R.id.error_text
            setTextColor(Color.BLACK)
        }
        addView(
            errorText,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
                marginEnd = Resources.dp(context, 8f).toInt()
                startToEnd = R.id.error_icon
                endToStart = R.id.error_action
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        setOnClickListener {
            overallAction?.invoke()
        }
        errorAction.setOnClickListener {
            onAction?.invoke()
        }
        errorClose.setOnClickListener {
            val reason = synchronized(this) {
                errors.firstOrNull()?.reason
            }
            if (reason != null) {
                dismiss(reason)
            }
        }
    }

    fun report(error: UserError) {
        if (!prefs.errors.canShowError(error.reason)) {
            return
        }
        synchronized(this) {
            errors.removeAll { it.reason == error.reason }
            errors.add(error)
            errors.sortBy { it.reason.id }
        }
        displayNextError()
        show()
    }

    fun dismiss(reason: ErrorBannerReason) {
        synchronized(this) {
            errors.removeAll { it.reason == reason }
        }
        displayNextError()
    }

    fun dismissAll() {
        synchronized(this) {
            errors.clear()
        }
        displayNextError()
    }

    private fun displayNextError() {
        val first = synchronized(this) {
            errors.firstOrNull()
        }
        if (first != null) {
            displayError(first)
        } else {
            onAction = null
            hide()
        }
    }

    private fun displayError(error: UserError) {
        errorText.text = error.title
        errorAction.text = error.action
        errorIcon.setImageResource(error.icon)
        onAction = error.onAction
        errorAction.isVisible = !error.action.isNullOrEmpty()
        overallAction = if (error.action == null) {
            error.onAction
        } else {
            null
        }
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

}
