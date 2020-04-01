package com.krootl.beetlens.ui.beetles.fab

import android.graphics.Color
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.krootl.beetlens.ui.common.gl.GLProgram
import com.krootl.beetlens.ui.common.gl.GLTextureView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * FooBar metaballs using OpenGL.
 */
class MetaballsGLRenderer(private val vertexShaderCode: String, private val fragmentShaderCode: String) : GLTextureView.Renderer {

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private lateinit var metaballs: Metaballs

    private val viewSize = PointF()

    var fooSize = 0f
    val fooPosition = PointF()
    var fooColor: FloatArray = Color.MAGENTA.let {
        floatArrayOf(it.red.toFloat(), it.green.toFloat(), it.blue.toFloat())
    }

    var barSize = 0f
    val barPosition = PointF()
    var barColor: FloatArray = Color.CYAN.let {
        floatArrayOf(it.red.toFloat(), it.green.toFloat(), it.blue.toFloat())
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        metaballs = Metaballs(vertexShaderCode, fragmentShaderCode)
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        metaballs.draw(mvpMatrix)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        viewSize.set(width.toFloat(), height.toFloat())

        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    inner class Metaballs(vertexShaderCode: String, fragmentShaderCode: String) : GLProgram(vertexShaderCode, fragmentShaderCode) {

        private var fooPositionHandle: Int = 0
        private var fooColorHandle: Int = 0
        private var fooSizeHandle: Int = 0

        private var barPositionHandle: Int = 0
        private var barSizeHandle: Int = 0
        private var barColorHandle: Int = 0

        init {
            fooPositionHandle = GLES20.glGetUniformLocation(program, "u_FooPosition")
            fooSizeHandle = GLES20.glGetUniformLocation(program, "u_FooSize")
            fooColorHandle = GLES20.glGetUniformLocation(program, "u_FooColor")

            barPositionHandle = GLES20.glGetUniformLocation(program, "u_BarPosition")
            barSizeHandle = GLES20.glGetUniformLocation(program, "u_BarSize")
            barColorHandle = GLES20.glGetUniformLocation(program, "u_BarColor")
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

            GLES20.glUniform1f(fooSizeHandle, fooSize)
            GLES20.glUniform2fv(fooPositionHandle, 1, floatArrayOf(fooPosition.x, fooPosition.y), 0)
            GLES20.glUniform3fv(fooColorHandle, 1, fooColor, 0)

            GLES20.glUniform1f(barSizeHandle, barSize)
            GLES20.glUniform2fv(barPositionHandle, 1, floatArrayOf(barPosition.x, barPosition.y), 0)
            GLES20.glUniform3fv(barColorHandle, 1, barColor, 0)

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