package com.krootl.beetlens.ui.beetles.lens

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.RawRes
import com.google.android.material.math.MathUtils.lerp
import com.krootl.beetlens.R
import com.krootl.beetlens.ui.common.gl.GLTextureView
import com.krootl.beetlens.ui.common.gl.ViewToGLRenderer
import kotlinx.android.synthetic.main.layout_lens_view.view.*
import kotlin.math.pow


class LensView
@JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val lensRadiusDefault = 0.3f // Fraction from view width

    private lateinit var viewToGlRenderer: LensGLRenderer
    val renderer: ViewToGLRenderer
        get() = viewToGlRenderer

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_lens_view, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        initLensGL()
    }

    /**
     * Configure [GLTextureView] with [LensGLRenderer].
     * Also make it transparent and render only when asked [GLTextureView.RENDERMODE_WHEN_DIRTY].
     */
    private fun initLensGL() {
        fun readTextFromRaw(@RawRes resourceId: Int) = resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }

        lensGLTextureView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        lensGLTextureView.setEGLContextClientVersion(2)

        viewToGlRenderer = LensGLRenderer(
            readTextFromRaw(R.raw.vertex_shader),
            readTextFromRaw(R.raw.lens_fragment_shader)
        )

        lensGLTextureView.setRenderer(viewToGlRenderer)
        lensGLTextureView.renderMode = GLTextureView.RENDERMODE_WHEN_DIRTY
        lensGLTextureView.isOpaque = false
    }

    fun updateLens(fabCenter: PointF, expandFraction: Float) {
        val lensRadiusFraction = expandFraction.pow(2)
        val positionFraction = expandFraction.pow(3)
        val alphaFraction = expandFraction.pow(4)

        val t = fabCenter.x / width

        val offsetHorizontal = positionFraction * (lensRadiusDefault * width) * 0.85f
        val offsetVertical = positionFraction * (lensRadiusDefault * width) * 0.85f

        val offsetX = lerp(-offsetHorizontal, offsetHorizontal, t)
        val offsetY = lerp3(offsetVertical, offsetVertical * 1.25f, offsetVertical, t)

        val lensPositionX = fabCenter.x - offsetX
        val lensPositionY = fabCenter.y - offsetY

        viewToGlRenderer.lensPosition.set(lensPositionX, lensPositionY)
        viewToGlRenderer.lensRadius = lensRadiusDefault * lensRadiusFraction
        viewToGlRenderer.lensAlpha = alphaFraction

        lensGLTextureView.requestRender()
    }

    /**
     * Linear interpolation between three values
     */
    private fun lerp3(a: Float, b: Float, c: Float, t: Float): Float {
        val newT: Float
        val tMin: Float
        val tMax: Float
        return if (t <= 0.5) {
            tMin = 0f
            tMax = 0.5f
            newT = (t - tMin) / (tMax - tMin)
            lerp(a, b, newT)
        } else {
            tMin = 0.5f
            tMax = 1f
            newT = (t - tMin) / (tMax - tMin)
            lerp(b, c, newT)
        }
    }
}