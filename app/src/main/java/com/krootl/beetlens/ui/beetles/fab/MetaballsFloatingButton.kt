package com.krootl.beetlens.ui.beetles.fab

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.math.MathUtils
import com.krootl.beetlens.R
import com.krootl.beetlens.ui.common.dpToPx
import com.krootl.beetlens.ui.common.gl.GLTextureView
import kotlinx.android.synthetic.main.layout_metaballs_floating_button.view.*
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow


/**
 * [MetaballsFloatingButton] consists of two entities — the SLOT and the FAB; both are metaballs. The slot position is static, it's also the fab default position.
 * One can drag the fab with touch, so the metaballs interact. The fab [setOnTouchListener] is overridden to add slow-down and [SpringAnimation] effects.
 * It also triggers action callbacks when the fab reaches either horizontal or vertical position threshold.
 * The fab can be detached if dragged upwards more than [dragUpwardsDistanceLimit]
 *
 * [MetaballsFloatingButton] uses [FabCrossfadeIcon] that supports both [DrawableRes] and [LottieAnimationView] via [RawRes] icons; color and size modification.
 */
class MetaballsFloatingButton
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var metaballsGLRenderer: MetaballsGLRenderer

    private lateinit var springAnimationSlideInY: SpringAnimation

    private lateinit var springAnimationFakeDragX: SpringAnimation
    private lateinit var springAnimationFakeDragY: SpringAnimation

    private lateinit var springAnimationDragX: SpringAnimation
    private lateinit var springAnimationDragY: SpringAnimation

    private val fabMetaballColor = Color.BLACK.let {
        floatArrayOf(it.red.toFloat(), it.green.toFloat(), it.blue.toFloat())
    }

    private val slotMetaballColor = ContextCompat.getColor(context, R.color.primaryColor).let {
        floatArrayOf(it.red.toFloat(), it.green.toFloat(), it.blue.toFloat())
    }

    private val slotSize = 40f.dpToPx()
    private val fabSize = 70f.dpToPx()

    private val fabDefaultTransform = RectF()
    private val fabCenter: PointF
        get() {
            return PointF(fab.x + fab.width / 2f, fab.y + fab.height / 2f)
        }

    private val dragUpwardsDistanceLimit = 80f.dpToPx()
    private val dragDistanceLimit = 56f.dpToPx()
    private var dragOriginX = 0f
    private var dragOriginY = 0f
    private var dragX = 0f
    private var dragY = 0f

    private var isFakeDraggingX = false
    private var isFakeDraggingY = false
    private val isFakeDragging: Boolean
        get() = isFakeDraggingX || isFakeDraggingY

    private val ignoreTouchEvents: Boolean
        get() = !userTouchEnabled || isFakeDragging

    var drawFabMetaball = true

    var onPositionChanged: ((PointF, PointF) -> Unit)? = null
    var onDragStateChanged: ((Boolean) -> Unit)? = null
    var onFakeDragEnd: (() -> Unit)? = null

    var userTouchEnabled = true
    var dragSlowDownEnabled = true

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_metaballs_floating_button, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        initMetaballsGL()

        setupSpringAnimations()
        setupTouchLogic()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (fabDefaultTransform.isEmpty) {
            fabDefaultTransform.set(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height)
        }
        updateMetaballs()
    }

    /**
     * Configure [GLTextureView] with [MetaballsGLRenderer].
     * Also make it transparent and render only when asked [GLTextureView.RENDERMODE_WHEN_DIRTY].
     */
    private fun initMetaballsGL() {
        fun readTextFromRaw(@RawRes resourceId: Int) = resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }

        metaballsGLTextureView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        metaballsGLTextureView.setEGLContextClientVersion(2)
        metaballsGLTextureView.isOpaque = false

        metaballsGLRenderer = MetaballsGLRenderer(
            readTextFromRaw(R.raw.vertex_shader),
            readTextFromRaw(R.raw.metaballs_fragment_shader)
        )

        metaballsGLTextureView.setRenderer(metaballsGLRenderer)
        metaballsGLTextureView.renderMode = GLTextureView.RENDERMODE_WHEN_DIRTY
    }

    private fun setupSpringAnimations() {
        springAnimationSlideInY = createSlideSpringAnimation(fab, DynamicAnimation.Y)

        springAnimationFakeDragX = createFakeDragSpringAnimation(fab, DynamicAnimation.X).apply {
            addEndListener { _, _, _, _ ->
                isFakeDraggingX = false
                if (!isFakeDragging) onFakeDragEnd?.invoke()
            }
        }
        springAnimationFakeDragY = createFakeDragSpringAnimation(fab, DynamicAnimation.Y).apply {
            addEndListener { _, _, _, _ ->
                isFakeDraggingY = false
                if (!isFakeDragging) onFakeDragEnd?.invoke()
            }
        }

        springAnimationDragX = createDragSpringAnimation(fab, DynamicAnimation.X)
        springAnimationDragY = createDragSpringAnimation(fab, DynamicAnimation.Y)
    }

    fun revealOnboardingHints() {
        fun reveal(vararg views: View) {
            views.forEach {
                it.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .translationX(0f)
                    .alpha(1f)
                    .setInterpolator(DecelerateInterpolator())
                    .duration = 500
            }
        }

        val scale = 0.85f
        val offset = 10f.dpToPx()
        imageHintLeft.apply {
            scaleX = scale
            scaleY = scale
            translationX = offset
        }
        imageHintUp.apply {
            scaleX = scale
            scaleY = scale
            translationY = offset
        }
        imageHintRight.apply {
            scaleX = scale
            scaleY = scale
            translationX = -offset
        }
        reveal(imageHintLeft, imageHintUp, imageHintRight)
    }

    fun hideOnboardingHint(left: Boolean = false, up: Boolean = false, right: Boolean = false) {
        fun hide(vararg views: View) {
            views.forEach {
                it.animate()
                    .alpha(0f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .duration = 500
            }
        }
        if (left && imageHintLeft.alpha > 0f) hide(imageHintLeft)
        if (up && imageHintUp.alpha > 0f) hide(imageHintUp)
        if (right && imageHintRight.alpha > 0f) hide(imageHintRight)
    }

    fun setFabIcon(icon: FabIcon) {
        fabIcon.setIcon(icon)
    }

    fun setSlotIcon(icon: FabIcon) {
        slotIcon.setIcon(icon)
    }

    fun performHapticFeedback() {
        fab.performHapticFeedback(
            11, /* [HapticFeedbackConstants.DRAG_CROSSING] */
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Animate "fake drag" and then return [fab] to [slot] position using [SpringAnimation].
     */
    fun fakeDragBy(dX: Float = 0f, dY: Float = 0f) {
        if (isFakeDragging) return
        isFakeDraggingX = true
        isFakeDraggingY = true

        fab.animate()
            .translationXBy(dX)
            .translationYBy(dY)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setUpdateListener {
                updateMetaballs()
            }
            .withEndAction {
                postDelayed({
                    springAnimationFakeDragX.animateToFinalPosition(fabDefaultTransform.left)
                    springAnimationFakeDragY.animateToFinalPosition(fabDefaultTransform.top)
                }, 360)
            }
    }

    /**
     * Reveal [fab] with the upwards [SpringAnimation].
     */
    fun showButton() {
        fab.y = height + fab.height.toFloat()
        metaballsGLTextureView.visibility = View.INVISIBLE
        visibility = View.VISIBLE
        post {
            springAnimationSlideInY.animateToFinalPosition(fabDefaultTransform.top)
        }
    }

    fun hideButton(animate: Boolean, duration: Long = 0) {
        if (!animate) {
            visibility = View.INVISIBLE
        } else {
            animate()
                .alpha(0f)
                .withEndAction {
                    visibility = View.INVISIBLE
                }
                .duration = duration
        }
    }

    /**
     * Let users drag [fab]; slow down drag if [dragSlowDownEnabled];
     * Let users detach [fab] in upwards direction after [dragUpwardsDistanceLimit] reached.
     *
     * Using [SpringAnimation] to bounce [fab] back to [slot] position after drag is over.
     */
    private fun setupTouchLogic() {
        var centerOffsetX = 0f
        var centerOffsetY = 0f
        var dX = 0f
        var dY = 0f
        val location = IntArray(2)

        fab.setOnTouchListener { view, event ->
            if (ignoreTouchEvents) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.getLocationOnScreen(location)
                    centerOffsetX = location[0] + view.width / 2 - event.rawX
                    centerOffsetY = location[1] + view.height / 2 - event.rawY

                    dX = view.x - event.rawX
                    dY = view.y - event.rawY

                    dragOriginX = event.rawX
                    dragOriginY = event.rawY

                    onDragStateChanged?.invoke(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX - centerOffsetX
                    var newY = event.rawY + dY - centerOffsetY

                    dragX = event.rawX - dragOriginX
                    dragY = event.rawY - dragOriginY

                    if (dragSlowDownEnabled) {
                        val distanceY = fabDefaultTransform.top - newY

                        // Allow to drag button upwards
                        val extraDistanceLimit = if (distanceY > dragUpwardsDistanceLimit)
                            (distanceY * min((distanceY - dragUpwardsDistanceLimit) / dragUpwardsDistanceLimit, 1f).pow(6))
                        else 0f

                        val distance = MathUtils.dist(fabDefaultTransform.left, fabDefaultTransform.top, newX, newY)
                        val distanceLimit = dragDistanceLimit + extraDistanceLimit.absoluteValue

                        newX = fabDefaultTransform.left +
                                (newX - fabDefaultTransform.left) * min(1f, distanceLimit / distance).pow(0.8f)
                        newY = fabDefaultTransform.top +
                                (newY - fabDefaultTransform.top) * min(1f, distanceLimit / distance).pow(0.8f)
                    }

                    springAnimationDragX.animateToFinalPosition(newX)
                    springAnimationDragY.animateToFinalPosition(newY)
                }
                MotionEvent.ACTION_UP -> {
                    springAnimationDragX.animateToFinalPosition(fabDefaultTransform.left)
                    springAnimationDragY.animateToFinalPosition(fabDefaultTransform.top)

                    onDragStateChanged?.invoke(false)
                }
            }
            return@setOnTouchListener true
        }
    }

    /**
     * Used for the reveal animation.
     */
    private fun createSlideSpringAnimation(view: View, property: DynamicAnimation.ViewProperty): SpringAnimation =
        SpringAnimation(view, property).setSpring(SpringForce().apply {
            stiffness = SpringForce.STIFFNESS_LOW
            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }).apply {
            addUpdateListener { _, _, _ -> updateMetaballs() }
            addEndListener { _, _, _, _ -> metaballsGLTextureView.visibility = View.VISIBLE }
        }

    /**
     * Used for the fake drag return animation.
     */
    private fun createFakeDragSpringAnimation(view: View, property: DynamicAnimation.ViewProperty): SpringAnimation =
        SpringAnimation(view, property).setSpring(SpringForce().apply {
            stiffness = 500f
            dampingRatio = 0.4f
        }).apply {
            addUpdateListener { _, _, _ -> updateMetaballs() }
        }

    /**
     * Used for the touch drag return animation.
     */
    private fun createDragSpringAnimation(view: View, property: DynamicAnimation.ViewProperty): SpringAnimation =
        SpringAnimation(view, property).setSpring(SpringForce().apply {
            stiffness = (SpringForce.STIFFNESS_MEDIUM + SpringForce.STIFFNESS_LOW) / 2f
            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }).apply { addUpdateListener { _, _, _ -> updateMetaballs() } }

    /**
     * Update GLRenderer with up-to-date metaball values and request render.
     */
    private fun updateMetaballs() {
        // SLOT
        metaballsGLRenderer.fooColor = slotMetaballColor
        metaballsGLRenderer.fooSize = slotSize
        metaballsGLRenderer.fooPosition.set(fabDefaultTransform.centerX(), fabDefaultTransform.centerY())
        // FAB
        metaballsGLRenderer.barColor = fabMetaballColor
        metaballsGLRenderer.barSize = if (drawFabMetaball) fabSize else 0f
        metaballsGLRenderer.barPosition.set(fabCenter.x, fabCenter.y)

        metaballsGLTextureView.requestRender()
        notifyPositionUpdated()
    }

    private fun notifyPositionUpdated() {
        val positionOffset = PointF(fabCenter.x - fabDefaultTransform.centerX(), fabCenter.y - fabDefaultTransform.centerY())
        onPositionChanged?.invoke(fabCenter, positionOffset)
    }
}