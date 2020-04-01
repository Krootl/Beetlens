package com.krootl.beetlens.ui.beetles.fab

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.krootl.beetlens.R
import kotlinx.android.synthetic.main.layout_fab_crossfade_icon.view.*

sealed class FabIcon {
    object Empty : FabIcon()

    abstract class DrawableIcon : FabIcon() {
        @get:DrawableRes
        abstract val resource: Int
    }

    data class Beetle(override val resource: Int = R.drawable.ic_beetle) : DrawableIcon()
    data class ArrowUp(override val resource: Int = R.drawable.ic_chevron_up) : DrawableIcon()
    data class ArrowLeft(override val resource: Int = R.drawable.ic_chevron_left) : DrawableIcon()
    data class ArrowRight(override val resource: Int = R.drawable.ic_chevron_right) : DrawableIcon()
    data class Close(override val resource: Int = R.drawable.ic_close) : DrawableIcon()


    abstract class LottieIcon : FabIcon() {
        @get:RawRes
        abstract val resource: Int
    }

    data class Maneki(override val resource: Int = R.raw.lottie_maneki) : LottieIcon()
    data class Kawaii(override val resource: Int = R.raw.lottie_kawaii) : LottieIcon()
    data class Tap(override val resource: Int = R.raw.lottie_tap) : LottieIcon()
    data class DragRight(override val resource: Int = R.raw.lottie_drag_right) : LottieIcon()
}

class FabCrossfadeIcon
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var icon: FabIcon = FabIcon.Empty
    private var isBackIconVisible = true

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_fab_crossfade_icon, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    fun setIcon(icon: FabIcon) {
        if (this.icon == icon) return
        this.icon = icon

        val minScale = 0.666f
        val duration = 180L

        isBackIconVisible = !isBackIconVisible

        fun View.fadeView(fadeOut: Boolean) {
            animate()
                .alpha(if (!fadeOut) 1f else 0f)
                .scaleX(if (!fadeOut) 1f else minScale)
                .scaleY(if (!fadeOut) 1f else minScale)
                .setInterpolator(FastOutLinearInInterpolator())
                .duration = duration
        }

        iconBack.fadeView(fadeOut = !isBackIconVisible)
        iconFront.fadeView(fadeOut = isBackIconVisible)

        fun updateIcon(view: LottieAnimationView, icon: FabIcon) {
            if (icon is FabIcon.Empty) {
                view.setImageDrawable(null)
                view.cancelAnimation()
            }
            if (icon is FabIcon.DrawableIcon) {
                view.setImageResource(icon.resource)
            } else if (icon is FabIcon.LottieIcon) {
                view.setAnimation(icon.resource)
                view.addValueCallback(
                    KeyPath("**"),
                    LottieProperty.COLOR_FILTER,
                    { PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP) }
                )
                view.repeatCount = LottieDrawable.INFINITE
                view.speed = .7f
                view.progress = 0f
                view.playAnimation()
            }
        }

        updateIcon(if (isBackIconVisible) iconBack else iconFront, icon)
    }
}