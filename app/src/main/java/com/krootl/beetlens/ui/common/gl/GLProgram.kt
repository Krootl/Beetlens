package com.krootl.beetlens.ui.common.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 2.0.
 */
open class GLProgram(vertexShaderCode: String, fragmentShaderCode: String) {

    protected val vertexBuffer: FloatBuffer
    protected val drawListBuffer: ShortBuffer

    protected val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices
    protected val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    protected val program: Int

    protected var mvpMatrixHandle: Int = 0
    protected var positionHandle: Int = 0
    protected var resolutionHandle: Int = 0

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    init {
        // Initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(squareCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(squareCoords)
        vertexBuffer.position(0)
        // Initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(drawOrder.size * 2)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(drawOrder)
        drawListBuffer.position(0)
        // Prepare shaders and OpenGL program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()             // create empty OpenGL Program
        GLES20.glAttachShader(program, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(program, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(program)                  // create OpenGL program executables

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        resolutionHandle = GLES20.glGetUniformLocation(program, "u_Resolution")
    }

    companion object {
        private val TAG = GLProgram::class.java.simpleName

        internal const val COORDS_PER_VERTEX = 3 // Number of coordinates per vertex in this array
        internal var squareCoords = floatArrayOf(
            -1f, 1f, 0.0f, // top left
            -1f, -1f, 0.0f, // bottom left
            1f, -1f, 0.0f, // bottom right
            1f, 1f, 0.0f // top right
        )

        /**
         * Utility method for compiling a OpenGL shader.
         *
         *
         * **Note:** When developing shaders, use the checkGlError()
         * method to debug shader coding errors.
         *
         * @param type - Vertex or fragment shader type.
         * @param shaderCode - String containing the shader code.
         * @return - Returns an id for the shader.
         */
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }

        /**
         * Utility method for debugging OpenGL calls. Provide the name of the call
         * just after making it:
         *
         * <pre>
         * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
         * checkGlError("glGetUniformLocation");</pre>
         *
         * If the operation is not successful, the check throws an error.
         *
         * @param glOperation - Name of the OpenGL call to check.
         */
        fun checkGlError(glOperation: String) {
            var hasError: Boolean
            do {
                val error = GLES20.glGetError()
                hasError = error != GLES20.GL_NO_ERROR
                if (hasError) {
                    Log.e(TAG, "$glOperation: glError $error")
                    throw RuntimeException("$glOperation: glError $error")
                }
            } while (hasError)
        }
    }
}