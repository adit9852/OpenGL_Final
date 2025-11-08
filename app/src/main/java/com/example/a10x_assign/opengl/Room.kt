package com.example.a10x_assign.opengl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.inject.Inject

class Room @Inject constructor() {
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec3 vNormal;
        varying vec3 vLighting;
        void main() {
            gl_Position = uMVPMatrix * vPosition;

            // Simple directional lighting
            vec3 lightDirection = normalize(vec3(0.5, 1.0, 0.8));
            vec3 ambientLight = vec3(0.4, 0.4, 0.45);
            vec3 directionalLight = vec3(0.6, 0.6, 0.55);

            float directional = max(dot(vNormal, lightDirection), 0.0);
            vLighting = ambientLight + (directionalLight * directional);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec3 uColor;
        varying vec3 vLighting;
        void main() {
            gl_FragColor = vec4(uColor * vLighting, 1.0);
        }
    """.trimIndent()

    private var program: Int = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var normalBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private var positionHandle: Int = 0
    private var normalHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    // Room dimensions
    private val width = 6f
    private val height = 4f
    private val depth = 8f

    private val vertices: FloatArray
    private val normals: FloatArray
    private val indices: ShortArray

    private var isInitialized = false

    init {
        vertices = createRoomVertices()
        normals = createRoomNormals()
        indices = createRoomIndices()
    }

    fun initialize() {
        if (isInitialized) return

        // Initialize vertex buffer
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Initialize normal buffer
        val nbb = ByteBuffer.allocateDirect(normals.size * 4)
        nbb.order(ByteOrder.nativeOrder())
        normalBuffer = nbb.asFloatBuffer()
        normalBuffer.put(normals)
        normalBuffer.position(0)

        // Initialize index buffer
        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)

        // Prepare shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        isInitialized = true
    }

    private fun createRoomVertices(): FloatArray {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        return floatArrayOf(
            // Floor vertices (0-3)
            -w, -h, -d,
             w, -h, -d,
             w, -h,  d,
            -w, -h,  d,

            // Ceiling vertices (4-7)
            -w,  h, -d,
             w,  h, -d,
             w,  h,  d,
            -w,  h,  d,

            // Back wall vertices (8-11)
            -w, -h, -d,
             w, -h, -d,
             w,  h, -d,
            -w,  h, -d,

            // Front wall vertices (12-15)
            -w, -h,  d,
             w, -h,  d,
             w,  h,  d,
            -w,  h,  d,

            // Left wall vertices (16-19)
            -w, -h, -d,
            -w, -h,  d,
            -w,  h,  d,
            -w,  h, -d,

            // Right wall vertices (20-23)
             w, -h, -d,
             w, -h,  d,
             w,  h,  d,
             w,  h, -d
        )
    }

    private fun createRoomNormals(): FloatArray {
        return floatArrayOf(
            // Floor normals (pointing up)
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f,

            // Ceiling normals (pointing down)
            0f, -1f, 0f,
            0f, -1f, 0f,
            0f, -1f, 0f,
            0f, -1f, 0f,

            // Back wall normals (pointing forward)
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,

            // Front wall normals (pointing backward)
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f,

            // Left wall normals (pointing right)
            1f, 0f, 0f,
            1f, 0f, 0f,
            1f, 0f, 0f,
            1f, 0f, 0f,

            // Right wall normals (pointing left)
            -1f, 0f, 0f,
            -1f, 0f, 0f,
            -1f, 0f, 0f,
            -1f, 0f, 0f
        )
    }

    private fun createRoomIndices(): ShortArray {
        return shortArrayOf(
            // Floor
            0, 1, 2, 0, 2, 3,
            // Ceiling
            4, 6, 5, 4, 7, 6,
            // Back wall
            8, 9, 10, 8, 10, 11,
            // Front wall
            12, 14, 13, 12, 15, 14,
            // Left wall
            16, 17, 18, 16, 18, 19,
            // Right wall
            20, 22, 21, 20, 23, 22
        )
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "vNormal")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw floor (light gray)
        GLES20.glUniform3f(colorHandle, 0.75f, 0.75f, 0.78f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(0))

        // Draw ceiling (off-white)
        GLES20.glUniform3f(colorHandle, 0.9f, 0.9f, 0.92f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(6))

        // Draw back wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(12))

        // Draw front wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(18))

        // Draw left wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(24))

        // Draw right wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer.position(30))

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }
}
