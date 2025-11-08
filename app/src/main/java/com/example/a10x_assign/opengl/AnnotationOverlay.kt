package com.example.a10x_assign.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.a10x_assign.data.AnnotationEntity
import com.example.a10x_assign.data.AnnotationType
import com.example.a10x_assign.data.WallType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.inject.Inject

class AnnotationOverlay @Inject constructor() {
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """.trimIndent()

    private var program: Int = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    private var isInitialized = false

    // Room dimensions (must match Room.kt)
    private val width = 6f
    private val height = 4f
    private val depth = 8f

    private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    fun initialize() {
        if (isInitialized) return

        // Prepare shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        // Initialize index buffer (reusable for all rectangles)
        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)

        isInitialized = true
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun drawAnnotations(vpMatrix: FloatArray, annotations: List<AnnotationEntity>) {
        if (!isInitialized || annotations.isEmpty()) return

        GLES20.glUseProgram(program)

        // Enable blending for semi-transparent annotations
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Disable depth writing but keep depth testing
        GLES20.glDepthMask(false)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        annotations.forEach { annotation ->
            drawAnnotation(vpMatrix, annotation)
        }

        // Re-enable depth writing
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawAnnotation(vpMatrix: FloatArray, annotation: AnnotationEntity) {
        val vertices = getAnnotationVertices(annotation)

        // Create vertex buffer
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Get color based on annotation type
        val color = getAnnotationColor(annotation.type)

        // Set up model matrix (slight offset from wall to prevent z-fighting)
        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        // Small offset towards camera to render above wall
        val offset = 0.01f
        when (annotation.wallType) {
            WallType.BACK_WALL -> Matrix.translateM(modelMatrix, 0, 0f, 0f, offset)
            WallType.FRONT_WALL -> Matrix.translateM(modelMatrix, 0, 0f, 0f, -offset)
            WallType.LEFT_WALL -> Matrix.translateM(modelMatrix, 0, offset, 0f, 0f)
            WallType.RIGHT_WALL -> Matrix.translateM(modelMatrix, 0, -offset, 0f, 0f)
            WallType.FLOOR -> Matrix.translateM(modelMatrix, 0, 0f, offset, 0f)
            WallType.CEILING -> Matrix.translateM(modelMatrix, 0, 0f, -offset, 0f)
        }

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        // Enable vertex array
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }

    private fun getAnnotationVertices(annotation: AnnotationEntity): FloatArray {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        // Convert normalized coordinates (0-1) to world coordinates
        val x = annotation.x
        val y = annotation.y
        val w_size = annotation.width
        val h_size = annotation.height

        return when (annotation.wallType) {
            WallType.FLOOR -> {
                val x1 = -w + (x * width)
                val x2 = x1 + (w_size * width)
                val z1 = -d + (y * depth)
                val z2 = z1 + (h_size * depth)
                floatArrayOf(
                    x1, -h, z1,
                    x2, -h, z1,
                    x2, -h, z2,
                    x1, -h, z2
                )
            }
            WallType.CEILING -> {
                val x1 = -w + (x * width)
                val x2 = x1 + (w_size * width)
                val z1 = -d + (y * depth)
                val z2 = z1 + (h_size * depth)
                floatArrayOf(
                    x1, h, z1,
                    x2, h, z1,
                    x2, h, z2,
                    x1, h, z2
                )
            }
            WallType.BACK_WALL -> {
                val x1 = -w + (x * width)
                val x2 = x1 + (w_size * width)
                val y1 = -h + (y * height)
                val y2 = y1 + (h_size * height)
                floatArrayOf(
                    x1, y1, -d,
                    x2, y1, -d,
                    x2, y2, -d,
                    x1, y2, -d
                )
            }
            WallType.FRONT_WALL -> {
                val x1 = -w + (x * width)
                val x2 = x1 + (w_size * width)
                val y1 = -h + (y * height)
                val y2 = y1 + (h_size * height)
                floatArrayOf(
                    x1, y1, d,
                    x2, y1, d,
                    x2, y2, d,
                    x1, y2, d
                )
            }
            WallType.LEFT_WALL -> {
                val z1 = -d + (x * depth)
                val z2 = z1 + (w_size * depth)
                val y1 = -h + (y * height)
                val y2 = y1 + (h_size * height)
                floatArrayOf(
                    -w, y1, z1,
                    -w, y1, z2,
                    -w, y2, z2,
                    -w, y2, z1
                )
            }
            WallType.RIGHT_WALL -> {
                val z1 = -d + (x * depth)
                val z2 = z1 + (w_size * depth)
                val y1 = -h + (y * height)
                val y2 = y1 + (h_size * height)
                floatArrayOf(
                    w, y1, z1,
                    w, y1, z2,
                    w, y2, z2,
                    w, y2, z1
                )
            }
        }
    }

    private fun getAnnotationColor(type: AnnotationType): FloatArray {
        return when (type) {
            AnnotationType.SPRAY_AREA -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.5f) // Semi-transparent red
            AnnotationType.SAND_AREA -> floatArrayOf(1.0f, 1.0f, 0.0f, 0.5f)  // Semi-transparent yellow
            AnnotationType.OBSTACLE -> floatArrayOf(1.0f, 0.5f, 0.0f, 0.5f)   // Semi-transparent orange
        }
    }
}
