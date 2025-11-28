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
        varying vec3 vNormalInterp;
        varying vec3 vPositionInterp;

        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vPositionInterp = vec3(vPosition);
            vNormalInterp = vNormal;

            // Enhanced multi-light setup matching the room
            // Main overhead light (soft white from above)
            vec3 lightDir1 = normalize(vec3(0.3, 1.0, 0.2));
            vec3 lightColor1 = vec3(0.9, 0.95, 1.0); // Cool white

            // Secondary light (warm from side)
            vec3 lightDir2 = normalize(vec3(-0.8, 0.6, 0.5));
            vec3 lightColor2 = vec3(1.0, 0.9, 0.8); // Warm white

            // Rim light (subtle back lighting for depth)
            vec3 lightDir3 = normalize(vec3(0.0, 0.3, -1.0));
            vec3 lightColor3 = vec3(0.6, 0.7, 0.9); // Cool blue tint

            // Calculate diffuse lighting for each light
            float diff1 = max(dot(vNormal, lightDir1), 0.0);
            float diff2 = max(dot(vNormal, lightDir2), 0.0) * 0.6;
            float diff3 = max(dot(vNormal, lightDir3), 0.0) * 0.4;

            // Enhanced ambient light (brighter for robot visibility)
            vec3 ambientLight = vec3(0.5, 0.52, 0.55);

            // Combine all lighting with slightly higher intensity for robot
            vLighting = ambientLight +
                       (lightColor1 * diff1 * 0.8) +
                       (lightColor2 * diff2 * 0.7) +
                       (lightColor3 * diff3 * 0.5);

            // Boost for robot visibility
            vLighting = clamp(vLighting, 0.4, 1.6);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec3 uColor;
        varying vec3 vLighting;
        varying vec3 vNormalInterp;
        varying vec3 vPositionInterp;

        void main() {
            // Add subtle ambient occlusion
            float ao = 1.0 - (0.1 * (1.0 - abs(vNormalInterp.y)));

            // Calculate final color with lighting and AO
            vec3 finalColor = uColor * vLighting * ao;

            // Add slight metallic shine for robot parts
            float shininess = 0.15 * (1.0 - abs(vNormalInterp.y));
            finalColor += vec3(shininess);

            // Enhance color vibrancy
            finalColor = pow(finalColor, vec3(0.92));

            gl_FragColor = vec4(finalColor, 1.0);
        }
    """.trimIndent()

    private var program: Int = 0

    private var positionHandle: Int = 0
    private var normalHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    // Robot part lists
    private val robotParts = mutableListOf<RobotPart>()

    private var isInitialized = false

    data class RobotPart(
        val vertices: FloatArray,
        val normals: FloatArray,
        val indices: ShortArray,
        val color: FloatArray,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val offsetZ: Float = 0f
    )

    init {
        createConstructionRobot()
    }

    fun initialize() {
        // Always reinitialize to handle context recreation
        // (OpenGL context is lost when app goes to background)

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

    private fun createConstructionRobot() {
        // Base platform (large flat base)
        robotParts.add(createBox(
            width = 0.5f, height = 0.08f, depth = 0.5f,
            color = floatArrayOf(0.3f, 0.3f, 0.3f), // Dark gray
            offsetY = 0.04f
        ))

        // Wheels (4 corners)
        val wheelRadius = 0.06f
        val wheelWidth = 0.08f
        val wheelOffset = 0.2f

        // Front left wheel
        robotParts.add(createWheel(wheelRadius, wheelWidth,
            floatArrayOf(0.1f, 0.1f, 0.1f),
            offsetX = -wheelOffset, offsetY = 0.0f, offsetZ = wheelOffset))
        // Front right wheel
        robotParts.add(createWheel(wheelRadius, wheelWidth,
            floatArrayOf(0.1f, 0.1f, 0.1f),
            offsetX = wheelOffset, offsetY = 0.0f, offsetZ = wheelOffset))
        // Back left wheel
        robotParts.add(createWheel(wheelRadius, wheelWidth,
            floatArrayOf(0.1f, 0.1f, 0.1f),
            offsetX = -wheelOffset, offsetY = 0.0f, offsetZ = -wheelOffset))
        // Back right wheel
        robotParts.add(createWheel(wheelRadius, wheelWidth,
            floatArrayOf(0.1f, 0.1f, 0.1f),
            offsetX = wheelOffset, offsetY = 0.0f, offsetZ = -wheelOffset))

        // Main body (yellow box on top of base)
        robotParts.add(createBox(
            width = 0.35f, height = 0.25f, depth = 0.35f,
            color = floatArrayOf(0.95f, 0.75f, 0.1f), // Construction yellow
            offsetY = 0.205f
        ))

        // Cabin/Head (smaller box on body)
        robotParts.add(createBox(
            width = 0.25f, height = 0.15f, depth = 0.25f,
            color = floatArrayOf(0.9f, 0.9f, 0.9f), // Light gray
            offsetY = 0.405f
        ))

        // Robotic arm base (cylinder on top)
        robotParts.add(createCylinder(
            radius = 0.08f, height = 0.12f,
            color = floatArrayOf(0.2f, 0.2f, 0.2f), // Dark gray
            offsetY = 0.54f
        ))

        // Arm segment 1 (lower arm)
        robotParts.add(createBox(
            width = 0.06f, height = 0.3f, depth = 0.06f,
            color = floatArrayOf(0.95f, 0.75f, 0.1f), // Yellow
            offsetX = 0.1f, offsetY = 0.75f, offsetZ = 0.0f
        ))

        // Arm joint
        robotParts.add(createCylinder(
            radius = 0.05f, height = 0.06f,
            color = floatArrayOf(0.2f, 0.2f, 0.2f),
            offsetX = 0.1f, offsetY = 0.9f, offsetZ = 0.0f
        ))

        // Arm segment 2 (upper arm)
        robotParts.add(createBox(
            width = 0.05f, height = 0.25f, depth = 0.05f,
            color = floatArrayOf(0.95f, 0.75f, 0.1f),
            offsetX = 0.15f, offsetY = 1.025f, offsetZ = 0.0f
        ))

        // End effector/gripper
        robotParts.add(createBox(
            width = 0.08f, height = 0.08f, depth = 0.08f,
            color = floatArrayOf(0.3f, 0.3f, 0.3f),
            offsetX = 0.15f, offsetY = 1.19f, offsetZ = 0.0f
        ))
    }

    private fun createBox(width: Float, height: Float, depth: Float,
                          color: FloatArray,
                          offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): RobotPart {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val vertices = floatArrayOf(
            // Front face
            -w, -h,  d,   w, -h,  d,   w,  h,  d,  -w,  h,  d,
            // Back face
            -w, -h, -d,  -w,  h, -d,   w,  h, -d,   w, -h, -d,
            // Top face
            -w,  h, -d,  -w,  h,  d,   w,  h,  d,   w,  h, -d,
            // Bottom face
            -w, -h, -d,   w, -h, -d,   w, -h,  d,  -w, -h,  d,
            // Right face
             w, -h, -d,   w,  h, -d,   w,  h,  d,   w, -h,  d,
            // Left face
            -w, -h, -d,  -w, -h,  d,  -w,  h,  d,  -w,  h, -d
        )

        val normals = floatArrayOf(
            // Front
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Top
            0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
            // Right
            1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
            // Left
            -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f
        )

        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,    // Front
            4, 5, 6, 4, 6, 7,    // Back
            8, 9, 10, 8, 10, 11,  // Top
            12, 13, 14, 12, 14, 15, // Bottom
            16, 17, 18, 16, 18, 19, // Right
            20, 21, 22, 20, 22, 23  // Left
        )

        return RobotPart(vertices, normals, indices, color, offsetX, offsetY, offsetZ)
    }

    private fun createCylinder(radius: Float, height: Float,
                               color: FloatArray,
                               offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f,
                               segments: Int = 12): RobotPart {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val h = height / 2
        val angleStep = (2.0 * Math.PI / segments).toFloat()

        // Generate side vertices
        for (i in 0..segments) {
            val angle = i * angleStep
            val x = radius * kotlin.math.cos(angle)
            val z = radius * kotlin.math.sin(angle)

            // Bottom vertex
            vertices.addAll(listOf(x, -h, z))
            normals.addAll(listOf(x / radius, 0f, z / radius))

            // Top vertex
            vertices.addAll(listOf(x, h, z))
            normals.addAll(listOf(x / radius, 0f, z / radius))
        }

        // Generate side indices
        for (i in 0 until segments) {
            val base = (i * 2).toShort()
            indices.addAll(listOf(
                base, (base + 1).toShort(), (base + 3).toShort(),
                base, (base + 3).toShort(), (base + 2).toShort()
            ))
        }

        // Add top and bottom caps
        val centerBottom = vertices.size / 3
        vertices.addAll(listOf(0f, -h, 0f))
        normals.addAll(listOf(0f, -1f, 0f))

        val centerTop = vertices.size / 3
        vertices.addAll(listOf(0f, h, 0f))
        normals.addAll(listOf(0f, 1f, 0f))

        // Bottom cap triangles
        for (i in 0 until segments) {
            indices.addAll(listOf(
                centerBottom.toShort(),
                (i * 2).toShort(),
                ((i + 1) * 2).toShort()
            ))
        }

        // Top cap triangles
        for (i in 0 until segments) {
            indices.addAll(listOf(
                centerTop.toShort(),
                ((i + 1) * 2 + 1).toShort(),
                (i * 2 + 1).toShort()
            ))
        }

        return RobotPart(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            indices.toShortArray(),
            color, offsetX, offsetY, offsetZ
        )
    }

    private fun createWheel(radius: Float, width: Float,
                            color: FloatArray,
                            offsetX: Float = 0f, offsetY: Float = 0f, offsetZ: Float = 0f): RobotPart {
        // A wheel is essentially a short, wide cylinder rotated 90 degrees
        return createCylinder(radius, width, color, offsetX, offsetY, offsetZ, segments = 8)
    }


    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun draw(vpMatrix: FloatArray, x: Float, y: Float, z: Float, rotationY: Float, size: Float = 1.0f) {
        if (!isInitialized) return

        GLES20.glUseProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "vNormal")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)

        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)

        // Set up base transformation
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

        // Draw each robot part
        for (part in robotParts) {
            val partModelMatrix = FloatArray(16)
            Matrix.setIdentityM(partModelMatrix, 0)

            // Apply robot position and rotation
            Matrix.translateM(partModelMatrix, 0, x, y, z)
            Matrix.rotateM(partModelMatrix, 0, rotationY, 0f, 1f, 0f)

            // Apply size scaling
            Matrix.scaleM(partModelMatrix, 0, size, size, size)

            // Apply part offset
            Matrix.translateM(partModelMatrix, 0, part.offsetX, part.offsetY, part.offsetZ)

            // Calculate final MVP matrix
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partModelMatrix, 0)

            // Create buffers for this part
            val vbb = ByteBuffer.allocateDirect(part.vertices.size * 4)
            vbb.order(ByteOrder.nativeOrder())
            val vertexBuffer = vbb.asFloatBuffer()
            vertexBuffer.put(part.vertices)
            vertexBuffer.position(0)

            val nbb = ByteBuffer.allocateDirect(part.normals.size * 4)
            nbb.order(ByteOrder.nativeOrder())
            val normalBuffer = nbb.asFloatBuffer()
            normalBuffer.put(part.normals)
            normalBuffer.position(0)

            val ibb = ByteBuffer.allocateDirect(part.indices.size * 2)
            ibb.order(ByteOrder.nativeOrder())
            val indexBuffer = ibb.asShortBuffer()
            indexBuffer.put(part.indices)
            indexBuffer.position(0)

            // Set attributes and uniforms
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform3f(colorHandle, part.color[0], part.color[1], part.color[2])

            // Draw the part
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, part.indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }
}
