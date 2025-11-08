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
        varying vec3 vNormalInterp;
        varying vec3 vPositionInterp;

        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vPositionInterp = vec3(vPosition);
            vNormalInterp = vNormal;

            // Enhanced multi-light setup
            // Main overhead light (soft white from above)
            vec3 lightDir1 = normalize(vec3(0.3, 1.0, 0.2));
            vec3 lightColor1 = vec3(0.9, 0.95, 1.0); // Cool white

            // Secondary light (warm from side)
            vec3 lightDir2 = normalize(vec3(-0.8, 0.6, 0.5));
            vec3 lightColor2 = vec3(1.0, 0.9, 0.8); // Warm white

            // Rim light (subtle back lighting)
            vec3 lightDir3 = normalize(vec3(0.0, 0.3, -1.0));
            vec3 lightColor3 = vec3(0.6, 0.7, 0.9); // Cool blue tint

            // Calculate diffuse lighting for each light
            float diff1 = max(dot(vNormal, lightDir1), 0.0);
            float diff2 = max(dot(vNormal, lightDir2), 0.0) * 0.6;
            float diff3 = max(dot(vNormal, lightDir3), 0.0) * 0.4;

            // Enhanced ambient light (brighter base)
            vec3 ambientLight = vec3(0.55, 0.58, 0.62);

            // Combine all lighting
            vLighting = ambientLight +
                       (lightColor1 * diff1 * 0.7) +
                       (lightColor2 * diff2) +
                       (lightColor3 * diff3);

            // Add slight boost to prevent too dark areas
            vLighting = clamp(vLighting, 0.3, 1.5);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec3 uColor;
        varying vec3 vLighting;
        varying vec3 vNormalInterp;
        varying vec3 vPositionInterp;

        void main() {
            // Add subtle ambient occlusion based on surface normal
            float ao = 1.0 - (0.15 * (1.0 - abs(vNormalInterp.y)));

            // Calculate final color with lighting and AO
            vec3 finalColor = uColor * vLighting * ao;

            // Add slight color boost for vibrancy
            finalColor = pow(finalColor, vec3(0.95));

            gl_FragColor = vec4(finalColor, 1.0);
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
    private val wallThickness = 0.15f  // Thickness for 3D mesh walls

    private var vertices: FloatArray
    private var normals: FloatArray
    private var indices: ShortArray

    // Render mode: 0 = flat, 1 = mesh, 2 = wireframe
    var renderMode = 0
        set(value) {
            if (field != value) {
                field = value
                // Recreate geometry based on mode
                when (value) {
                    0 -> { // Flat
                        vertices = createRoomVertices()
                        normals = createRoomNormals()
                        indices = createRoomIndices()
                    }
                    1 -> { // Mesh
                        vertices = createMeshWallVertices()
                        normals = createMeshWallNormals()
                        indices = createMeshWallIndices()
                    }
                    2 -> { // Wireframe
                        vertices = createMeshWallVertices()
                        normals = createMeshWallNormals()
                        indices = createWireframeIndices()
                    }
                }

                // Update buffers if already initialized
                if (isInitialized) {
                    updateBuffers()
                }
            }
        }

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

    private fun createMeshWallVertices(): FloatArray {
        val w = width / 2
        val h = height / 2
        val d = depth / 2
        val t = wallThickness

        val vertices = mutableListOf<Float>()

        // Floor with thickness (8 vertices per face)
        // Inner face (top)
        vertices.addAll(listOf(-w, -h, -d, w, -h, -d, w, -h, d, -w, -h, d))
        // Outer face (bottom)
        vertices.addAll(listOf(-w, -h - t, -d, w, -h - t, -d, w, -h - t, d, -w, -h - t, d))
        // Edge faces (4 sides of thickness)
        vertices.addAll(listOf(-w, -h, -d, w, -h, -d, w, -h - t, -d, -w, -h - t, -d)) // Back edge
        vertices.addAll(listOf(w, -h, -d, w, -h, d, w, -h - t, d, w, -h - t, -d)) // Right edge
        vertices.addAll(listOf(w, -h, d, -w, -h, d, -w, -h - t, d, w, -h - t, d)) // Front edge
        vertices.addAll(listOf(-w, -h, d, -w, -h, -d, -w, -h - t, -d, -w, -h - t, d)) // Left edge

        // Ceiling with thickness (8 vertices per face)
        // Inner face (bottom)
        vertices.addAll(listOf(-w, h, -d, w, h, -d, w, h, d, -w, h, d))
        // Outer face (top)
        vertices.addAll(listOf(-w, h + t, -d, w, h + t, -d, w, h + t, d, -w, h + t, d))
        // Edge faces
        vertices.addAll(listOf(-w, h, -d, w, h, -d, w, h + t, -d, -w, h + t, -d))
        vertices.addAll(listOf(w, h, -d, w, h, d, w, h + t, d, w, h + t, -d))
        vertices.addAll(listOf(w, h, d, -w, h, d, -w, h + t, d, w, h + t, d))
        vertices.addAll(listOf(-w, h, d, -w, h, -d, -w, h + t, -d, -w, h + t, d))

        // Back wall with thickness
        // Inner face (front)
        vertices.addAll(listOf(-w, -h, -d, w, -h, -d, w, h, -d, -w, h, -d))
        // Outer face (back)
        vertices.addAll(listOf(-w, -h, -d - t, w, -h, -d - t, w, h, -d - t, -w, h, -d - t))
        // Edge faces
        vertices.addAll(listOf(-w, -h, -d, w, -h, -d, w, -h, -d - t, -w, -h, -d - t))
        vertices.addAll(listOf(w, -h, -d, w, h, -d, w, h, -d - t, w, -h, -d - t))
        vertices.addAll(listOf(w, h, -d, -w, h, -d, -w, h, -d - t, w, h, -d - t))
        vertices.addAll(listOf(-w, h, -d, -w, -h, -d, -w, -h, -d - t, -w, h, -d - t))

        // Front wall with thickness
        // Inner face (back)
        vertices.addAll(listOf(-w, -h, d, w, -h, d, w, h, d, -w, h, d))
        // Outer face (front)
        vertices.addAll(listOf(-w, -h, d + t, w, -h, d + t, w, h, d + t, -w, h, d + t))
        // Edge faces
        vertices.addAll(listOf(-w, -h, d, w, -h, d, w, -h, d + t, -w, -h, d + t))
        vertices.addAll(listOf(w, -h, d, w, h, d, w, h, d + t, w, -h, d + t))
        vertices.addAll(listOf(w, h, d, -w, h, d, -w, h, d + t, w, h, d + t))
        vertices.addAll(listOf(-w, h, d, -w, -h, d, -w, -h, d + t, -w, h, d + t))

        // Left wall with thickness
        // Inner face (right)
        vertices.addAll(listOf(-w, -h, -d, -w, -h, d, -w, h, d, -w, h, -d))
        // Outer face (left)
        vertices.addAll(listOf(-w - t, -h, -d, -w - t, -h, d, -w - t, h, d, -w - t, h, -d))
        // Edge faces
        vertices.addAll(listOf(-w, -h, -d, -w, -h, d, -w - t, -h, d, -w - t, -h, -d))
        vertices.addAll(listOf(-w, -h, d, -w, h, d, -w - t, h, d, -w - t, -h, d))
        vertices.addAll(listOf(-w, h, d, -w, h, -d, -w - t, h, -d, -w - t, h, d))
        vertices.addAll(listOf(-w, h, -d, -w, -h, -d, -w - t, -h, -d, -w - t, h, -d))

        // Right wall with thickness
        // Inner face (left)
        vertices.addAll(listOf(w, -h, -d, w, -h, d, w, h, d, w, h, -d))
        // Outer face (right)
        vertices.addAll(listOf(w + t, -h, -d, w + t, -h, d, w + t, h, d, w + t, h, -d))
        // Edge faces
        vertices.addAll(listOf(w, -h, -d, w, -h, d, w + t, -h, d, w + t, -h, -d))
        vertices.addAll(listOf(w, -h, d, w, h, d, w + t, h, d, w + t, -h, d))
        vertices.addAll(listOf(w, h, d, w, h, -d, w + t, h, -d, w + t, h, d))
        vertices.addAll(listOf(w, h, -d, w, -h, -d, w + t, -h, -d, w + t, h, -d))

        return vertices.toFloatArray()
    }

    private fun createMeshWallNormals(): FloatArray {
        val normals = mutableListOf<Float>()

        // Floor
        // Inner face (up)
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        // Outer face (down)
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) } // Back edge
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }  // Right edge
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }  // Front edge
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) } // Left edge

        // Ceiling
        // Inner face (down)
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        // Outer face (up)
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) }
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) }

        // Back wall
        // Inner face (forward)
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }
        // Outer face (backward)
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) }

        // Front wall
        // Inner face (backward)
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) }
        // Outer face (forward)
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) }

        // Left wall
        // Inner face (right)
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }
        // Outer face (left)
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) }

        // Right wall
        // Inner face (left)
        repeat(4) { normals.addAll(listOf(-1f, 0f, 0f)) }
        // Outer face (right)
        repeat(4) { normals.addAll(listOf(1f, 0f, 0f)) }
        // Edges
        repeat(4) { normals.addAll(listOf(0f, -1f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 0f, 1f)) }
        repeat(4) { normals.addAll(listOf(0f, 1f, 0f)) }
        repeat(4) { normals.addAll(listOf(0f, 0f, -1f)) }

        return normals.toFloatArray()
    }

    private fun createMeshWallIndices(): ShortArray {
        val indices = mutableListOf<Short>()
        var offset: Short = 0

        // Each wall has 6 faces (2 main faces + 4 edge faces), each face has 2 triangles (6 indices)
        for (wall in 0 until 6) {
            for (face in 0 until 6) {
                val base = (offset + face * 4).toShort()
                indices.addAll(listOf(base, (base + 1).toShort(), (base + 2).toShort()))
                indices.addAll(listOf(base, (base + 2).toShort(), (base + 3).toShort()))
            }
            offset = (offset + 24).toShort() // 24 vertices per wall (6 faces * 4 vertices)
        }

        return indices.toShortArray()
    }

    private fun createWireframeIndices(): ShortArray {
        val indices = mutableListOf<Short>()
        var offset: Short = 0

        // For wireframe, draw lines instead of triangles
        // Each face (quad) needs 4 lines (edges) but we only want unique edges
        for (wall in 0 until 6) {
            for (face in 0 until 6) {
                val base = (offset + face * 4).toShort()
                // Draw the 4 edges of each quad
                indices.addAll(listOf(base, (base + 1).toShort())) // Edge 0-1
                indices.addAll(listOf((base + 1).toShort(), (base + 2).toShort())) // Edge 1-2
                indices.addAll(listOf((base + 2).toShort(), (base + 3).toShort())) // Edge 2-3
                indices.addAll(listOf((base + 3).toShort(), base)) // Edge 3-0
            }
            offset = (offset + 24).toShort() // 24 vertices per wall (6 faces * 4 vertices)
        }

        return indices.toShortArray()
    }

    private fun updateBuffers() {
        // Update vertex buffer
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Update normal buffer
        val nbb = ByteBuffer.allocateDirect(normals.size * 4)
        nbb.order(ByteOrder.nativeOrder())
        normalBuffer = nbb.asFloatBuffer()
        normalBuffer.put(normals)
        normalBuffer.position(0)

        // Update index buffer
        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)
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

        when (renderMode) {
            0 -> drawFlat()
            1 -> drawMesh()
            2 -> drawWireframe()
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    private fun drawFlat() {
        // Draw flat walls - 2 triangles per wall = 6 indices per wall
        indexBuffer.position(0)

        // Draw floor (light gray)
        GLES20.glUniform3f(colorHandle, 0.75f, 0.75f, 0.78f)
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw ceiling (off-white)
        GLES20.glUniform3f(colorHandle, 0.9f, 0.9f, 0.92f)
        indexBuffer.position(6)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw back wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        indexBuffer.position(12)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw front wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        indexBuffer.position(18)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw left wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        indexBuffer.position(24)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw right wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        indexBuffer.position(30)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Reset buffer position
        indexBuffer.position(0)
    }

    private fun drawMesh() {
        // Draw mesh walls - each wall has 6 faces with 2 triangles each = 36 indices per wall
        val indicesPerWall = 36

        // Draw floor (light gray)
        GLES20.glUniform3f(colorHandle, 0.75f, 0.75f, 0.78f)
        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw ceiling (off-white)
        GLES20.glUniform3f(colorHandle, 0.9f, 0.9f, 0.92f)
        indexBuffer.position(indicesPerWall)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw back wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        indexBuffer.position(indicesPerWall * 2)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw front wall (light blue)
        GLES20.glUniform3f(colorHandle, 0.82f, 0.85f, 0.88f)
        indexBuffer.position(indicesPerWall * 3)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw left wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        indexBuffer.position(indicesPerWall * 4)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Draw right wall (soft beige)
        GLES20.glUniform3f(colorHandle, 0.88f, 0.84f, 0.8f)
        indexBuffer.position(indicesPerWall * 5)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesPerWall, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Reset buffer position
        indexBuffer.position(0)
    }

    private fun drawWireframe() {
        // Disable depth test to see all lines
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        // Set line width for better visibility
        GLES20.glLineWidth(3.0f)

        // Draw wireframe - each wall has 6 faces with 4 edges each = 48 indices per wall (24 lines)
        val linesPerFace = 8 // 4 edges * 2 indices per line
        val facesPerWall = 6
        val indicesPerWall = linesPerFace * facesPerWall

        // Use bright cyan for wireframe to make it visible
        GLES20.glUniform3f(colorHandle, 0.0f, 1.0f, 1.0f) // Cyan lines

        // Reset buffer position before drawing
        indexBuffer.position(0)

        // Draw all walls as wireframe
        for (wall in 0 until 6) {
            indexBuffer.position(wall * indicesPerWall)
            GLES20.glDrawElements(
                GLES20.GL_LINES,
                indicesPerWall,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
            )
        }

        // Reset buffer position after drawing
        indexBuffer.position(0)

        // Re-enable depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}
