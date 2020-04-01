package com.krootl.beetlens.ui.beetles.lens

import android.graphics.PointF
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import com.krootl.beetlens.ui.common.dpToPx
import com.krootl.beetlens.ui.common.gl.GLProgram
import com.krootl.beetlens.ui.common.gl.ViewToGLRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Magnifying glass aka lens using OpenGL.
 */
class LensGLRenderer(private val vertexShaderCode: String, private val fragmentShaderCode: String) : ViewToGLRenderer() {

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private lateinit var lens: Lens

    private val viewSize = PointF()

    // XY position in the parent.
    val lensPosition = PointF()

    // Lens renderer output alpha channel value.
    var lensAlpha = 0f

    // Fraction of the parent width.
    var lensRadius = 0f

    // Play around with these values to change lens magnifying glass appearance.
    var lensZoom = 0.4f
    var lensBend = 0.8f

    // Black border around the lens.
    var lensBorderWidth = 3f.dpToPx()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        super.onSurfaceCreated(gl, config)
        lens = Lens(vertexShaderCode, fragmentShaderCode)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)

        GLES20.glViewport(0, 0, width, height)
        viewSize.set(width.toFloat(), height.toFloat())

        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onDrawFrame(gl: GL10) {
        super.onDrawFrame(gl)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        lens.draw(mvpMatrix)
    }


    inner class Lens(vertexShaderCode: String, fragmentShaderCode: String) : GLProgram(vertexShaderCode, fragmentShaderCode) {

        private var textureUniformHandle: Int = 0
        private var lensPositionHandle: Int = 0
        private var lensRadiusHandle: Int = 0
        private var lensAlphaHandle: Int = 0
        private var lensZoomHandle: Int = 0
        private var lensBendHandle: Int = 0
        private var lensBorderWidthHandle: Int = 0

        init {
            textureUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture")
            lensPositionHandle = GLES20.glGetUniformLocation(program, "u_LensPosition")
            lensRadiusHandle = GLES20.glGetUniformLocation(program, "u_LensRadius")
            lensAlphaHandle = GLES20.glGetUniformLocation(program, "u_LensAlpha")
            lensZoomHandle = GLES20.glGetUniformLocation(program, "u_LensZoom")
            lensBendHandle = GLES20.glGetUniformLocation(program, "u_LensBend")
            lensBorderWidthHandle = GLES20.glGetUniformLocation(program, "u_LensBorderWidth")
        }

        fun draw(mvpMatrix: FloatArray) {
            // Add program to OpenGL environment
            GLES20.glUseProgram(program)
            // get handle to vertex shader's vPosition member
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(positionHandle)
            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

            GLES20.glUniform2fv(resolutionHandle, 1, floatArrayOf(viewSize.x, viewSize.y), 0)

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, glSurfaceTexture)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glUniform1i(textureUniformHandle, 0)

            GLES20.glUniform2fv(lensPositionHandle, 1, floatArrayOf(lensPosition.x, lensPosition.y), 0)
            GLES20.glUniform1f(lensRadiusHandle, lensRadius)
            GLES20.glUniform1f(lensAlphaHandle, lensAlpha)

            GLES20.glUniform1f(lensZoomHandle, lensZoom)
            GLES20.glUniform1f(lensBendHandle, lensBend)
            GLES20.glUniform1f(lensBorderWidthHandle, lensBorderWidth)

            // get handle to shape's transformation matrix
            checkGlError("glGetUniformLocation")
            // Apply the projection and view transformation
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            checkGlError("glUniformMatrix4fv")
            // Draw the square
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }
}