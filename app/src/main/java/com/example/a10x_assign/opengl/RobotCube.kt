package com.example.a10x_assign.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.inject.Inject

class RobotCube @Inject constructor() {
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec3 vNormal;
        varying vec3 vLighting;
        void main() {
            gl_Position = uMVPMatrix * vPosition;

            // Simple directional lighting
            vec3 lightDirection = normalize(vec3(0.5, 1.0, 0.8));
            vec3 ambientLight = vec3(0.3, 0.3, 0.3);
            vec3 directionalLight = vec3(0.7, 0.7, 0.7);

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

    // Robot dimensions (representing UR10e)
    private val size = 0.5f

    private val vertices: FloatArray
    private val normals: FloatArray
    private val indices: ShortArray

    private var isInitialized = false

    init {
        vertices = createCubeVertices()
        normals = createCubeNormals()
        indices = createCubeIndices()
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

    private fun createCubeVertices(): FloatArray {
        val s = size / 2
        return floatArrayOf(
            // Front face
            -s, -s,  s,
             s, -s,  s,
             s,  s,  s,
            -s,  s,  s,
            // Back face
            -s, -s, -s,
            -s,  s, -s,
             s,  s, -s,
             s, -s, -s,
            // Top face
            -s,  s, -s,
            -s,  s,  s,
             s,  s,  s,
             s,  s, -s,
            // Bottom face
            -s, -s, -s,
             s, -s, -s,
             s, -s,  s,
            -s, -s,  s,
            // Right face
             s, -s, -s,
             s,  s, -s,
             s,  s,  s,
             s, -s,  s,
            // Left face
            -s, -s, -s,
            -s, -s,  s,
            -s,  s,  s,
            -s,  s, -s
        )
    }

    private fun createCubeNormals(): FloatArray {
        return floatArrayOf(
            // Front
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            // Back
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f,
            0f, 0f, -1f,
            // Top
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f,
            0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,
            0f, -1f, 0f,
            0f, -1f, 0f,
            0f, -1f, 0f,
            // Right
            1f, 0f, 0f,
            1f, 0f, 0f,
            1f, 0f, 0f,
            1f, 0f, 0f,
            // Left
            -1f, 0f, 0f,
            -1f, 0f, 0f,
            -1f, 0f, 0f,
            -1f, 0f, 0f
        )
    }

    private fun createCubeIndices(): ShortArray {
        return shortArrayOf(
            0, 1, 2, 0, 2, 3,    // Front
            4, 5, 6, 4, 6, 7,    // Back
            8, 9, 10, 8, 10, 11,  // Top
            12, 13, 14, 12, 14, 15, // Bottom
            16, 17, 18, 16, 18, 19, // Right
            20, 21, 22, 20, 22, 23  // Left
        )
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun draw(vpMatrix: FloatArray, x: Float, y: Float, z: Float, rotationY: Float) {
        if (!isInitialized) return

        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

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

        // Orange color for robot
        GLES20.glUniform3f(colorHandle, 1.0f, 0.5f, 0.0f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }
}
