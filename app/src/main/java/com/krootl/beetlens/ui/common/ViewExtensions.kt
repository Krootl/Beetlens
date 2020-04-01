package com.krootl.beetlens.ui.common

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.get
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.roundToInt


private fun ViewPager2.getPageAtPosition(position: Int): View? {
    return if (position in 0 until (adapter?.itemCount ?: 0))
        ((this[0] as RecyclerView).layoutManager as LinearLayoutManager).findViewByPosition(position)
    else null
}

fun EditText.onChange(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            callback(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun TextView.strikeThrough(show: Boolean) {
    paintFlags = if (show) (paintFlags or Paint.STRIKE_THRU_TEXT_FLAG)
    else (paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv())
}

fun SeekBar.onChange(callback: (Float) -> Unit) {
    this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            callback(progress / 100f)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}

    })
}

fun Spinner.onItemSelected(callback: (Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>) {}
        override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
            callback(pos)
        }
    }
}

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun Group.makeVisible(visible: Boolean) {
    isInvisible = !visible
    updatePreLayout(parent as ConstraintLayout)
}

fun Int.pxToDp(): Int {
    val metrics = Resources.getSystem().displayMetrics
    val dp = this / (metrics.densityDpi / 160f)
    return dp.roundToInt()
}

fun Int.dpToPx(): Int {
    val metrics = Resources.getSystem().displayMetrics
    val px = this * (metrics.densityDpi / 160f)
    return px.roundToInt()
}

fun Float.dpToPx(): Float {
    val metrics = Resources.getSystem().displayMetrics
    return this * (metrics.densityDpi / 160f)
}

fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding, InitialMargin) -> Unit) {
    // Create a snapshot of the view's padding STATE
    val initialPadding = recordInitialPaddingForView(this)
    // Create a snapshot of the view's margin STATE
    val initialMargin = recordInitialMarginForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding STATE
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, initialPadding, initialMargin)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

data class InitialPadding(
    val left: Int, val top: Int,
    val right: Int, val bottom: Int
)

data class InitialMargin(
    val left: Int, val top: Int,
    val right: Int, val bottom: Int
)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

private fun recordInitialMarginForView(view: View): InitialMargin {
    val lp = view.layoutParams
    with(lp as ViewGroup.MarginLayoutParams) {
        return InitialMargin(
            leftMargin, topMargin, rightMargin, bottomMargin
        )
    }
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun ViewGroup.pleaseGetChild(at: Int): View? = if (at in 0 until childCount) this[at] else null

fun View.center(origin: ViewGroup? = null): Point {

    if (origin != null) {
        val offsetViewBounds = Rect()
        getDrawingRect(offsetViewBounds)
        origin.offsetDescendantRectToMyCoords(this, offsetViewBounds)
        return Point(offsetViewBounds.centerX(), offsetViewBounds.centerY())
    }

    return Point(x.toInt() + width / 2, y.toInt() + height / 2)
}

fun View.centerF(origin: ViewGroup? = null): PointF {
    return PointF(center(origin = origin))
}