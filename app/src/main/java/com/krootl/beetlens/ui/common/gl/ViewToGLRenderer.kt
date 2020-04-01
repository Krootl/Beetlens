package com.krootl.beetlens.ui.common.gl

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import android.view.Surface
import androidx.annotation.CallSuper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class ViewToGLRenderer : GLTextureView.Renderer {
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    var glSurfaceTexture = 0
        private set
    private var surfaceCanvas: Canvas? = null

    @CallSuper
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
    }

    private val surfaceSize = Point()

    @CallSuper
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        if (surfaceSize.equals(width, height)) return
        surfaceSize.set(width, height)

        releaseSurface()
        glSurfaceTexture = createTexture()

        if (glSurfaceTexture > 0) {
            //attach the texture to a surface.
            //It's a clue class for rendering an android view to gl level
            surfaceTexture = SurfaceTexture(glSurfaceTexture).apply { setDefaultBufferSize(width, height) }
            surface = Surface(surfaceTexture)
        }
    }

    @CallSuper
    override fun onDrawFrame(gl: GL10) {
        synchronized(this) {
            // update texture
//            Log.d(TAG, "onDrawFrame: ")
            surfaceTexture?.updateTexImage()
        }
    }

    fun releaseSurface() {
        surface?.release()
        surfaceTexture?.release()
        surface = null
        surfaceTexture = null
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)

        // Generate the texture to where android view will be rendered
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(1, textures, 0)
        checkGlError("Texture generate")
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        checkGlError("Texture bind")
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    fun onDrawViewBegin(): Canvas? {
        surfaceCanvas = null
        surface?.let {
            try {
                surfaceCanvas = it.lockCanvas(null)
            } catch (e: Exception) {
                Log.e(TAG, "error while rendering view to gl: $e")
            }
        }
        return surfaceCanvas
    }

    fun onDrawViewEnd() {
        surfaceCanvas?.let {
            surface?.unlockCanvasAndPost(it)
        }
        surfaceCanvas = null
    }

    fun checkGlError(op: String) {
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + GLUtils.getEGLErrorString(error))
        }
    }

    companion object {
        private val TAG = ViewToGLRenderer::class.java.simpleName
    }
}