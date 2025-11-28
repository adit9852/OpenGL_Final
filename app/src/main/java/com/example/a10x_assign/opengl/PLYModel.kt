package com.example.a10x_assign.opengl

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * PLYModel class to render PLY point cloud or mesh with advanced lighting
 * Replaces the Room class to render a scanned 3D model instead of walls
 */
class PLYModel(private val context: Context) {

    private var vertexBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private var normalBuffer: FloatBuffer? = null
    private var vertexCount = 0

    // Bounding box of the model
    var boundingBox: PLYLoader.BoundingBox? = null
        private set

    // Actual dimensions after scaling (for annotations and raycasting)
    private var actualWidth: Float = ROOM_WIDTH
    private var actualHeight: Float = ROOM_HEIGHT
    private var actualDepth: Float = ROOM_DEPTH

    // Model transformation matrix to align PLY to room coordinates
    private var modelMatrix = FloatArray(16)

    // Room dimensions - PLY model will be scaled to EXACTLY these dimensions
    // This ensures annotations and raycasting work perfectly
    private val targetWidth = 9f
    private val targetHeight = 6f
    private val targetDepth = 12f

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var normalHandle = 0
    private var mvpMatrixHandle = 0
    private var modelMatrixHandle = 0
    private var renderModeHandle = 0

    // Lighting uniform handles
    private var ambientColorHandle = 0
    private var lightPos1Handle = 0
    private var lightColor1Handle = 0
    private var lightPos2Handle = 0
    private var lightColor2Handle = 0
    private var lightPos3Handle = 0
    private var lightColor3Handle = 0
    private var viewPosHandle = 0

    // Render mode: 0 = wireframe (points), 1 = mesh (filled triangles)
    @Volatile
    var renderMode = 0
        set(value) {
            field = value.coerceIn(0, 1)
        }

    // Vertex shader - with lighting support
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform int uRenderMode;
        attribute vec4 vPosition;
        attribute vec4 vColor;
        attribute vec3 vNormal;
        varying vec3 fragNormal;
        varying vec4 fragColor;
        varying vec3 fragPosition;

        void main() {
            // MVP matrix already includes model transform
            gl_Position = uMVPMatrix * vPosition;

            // Different point sizes for render modes
            if (uRenderMode == 0) {
                gl_PointSize = 2.0;  // Wireframe mode
            } else {
                gl_PointSize = 5.0;  // Mesh mode
            }

            // Transform normal to world space
            fragNormal = normalize(mat3(uModelMatrix) * vNormal);
            fragPosition = vec3(uModelMatrix * vPosition);
            fragColor = vColor;
        }
    """.trimIndent()

    // Fragment shader - with enhanced multi-light system and subtle specular
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec3 fragNormal;
        varying vec4 fragColor;
        varying vec3 fragPosition;

        uniform vec3 uAmbientColor;
        uniform vec3 uLightPos1;
        uniform vec3 uLightColor1;
        uniform vec3 uLightPos2;
        uniform vec3 uLightColor2;
        uniform vec3 uLightPos3;
        uniform vec3 uLightColor3;
        uniform vec3 uViewPos;

        void main() {
            // Ambient lighting with slight boost
            vec3 ambient = uAmbientColor * fragColor.rgb;

            // Diffuse and specular lighting from 3 light sources
            vec3 norm = normalize(fragNormal);
            vec3 viewDir = normalize(uViewPos - fragPosition);

            // Light 1 - Main key light with subtle specular
            vec3 lightDir1 = normalize(uLightPos1 - fragPosition);
            float diff1 = max(dot(norm, lightDir1), 0.0);
            vec3 diffuse1 = diff1 * uLightColor1 * fragColor.rgb;

            // Subtle specular highlight for depth
            vec3 halfwayDir1 = normalize(lightDir1 + viewDir);
            float spec1 = pow(max(dot(norm, halfwayDir1), 0.0), 16.0);
            vec3 specular1 = spec1 * uLightColor1 * 0.15;

            // Light 2 - Fill light (softer, no specular)
            vec3 lightDir2 = normalize(uLightPos2 - fragPosition);
            float diff2 = max(dot(norm, lightDir2), 0.0);
            vec3 diffuse2 = diff2 * uLightColor2 * fragColor.rgb;

            // Light 3 - Rim light (subtle, for depth)
            vec3 lightDir3 = normalize(uLightPos3 - fragPosition);
            float diff3 = max(dot(norm, lightDir3), 0.0);
            vec3 diffuse3 = diff3 * uLightColor3 * fragColor.rgb;

            // Combine all lighting components
            vec3 lighting = ambient + diffuse1 + diffuse2 + diffuse3 + specular1;

            // Apply subtle tone mapping for professional look
            vec3 finalColor = lighting * 1.15;
            finalColor = finalColor / (finalColor + vec3(1.0)); // Reinhard tone mapping
            finalColor = pow(finalColor, vec3(1.0/2.2)); // Gamma correction

            gl_FragColor = vec4(finalColor, fragColor.a);
        }
    """.trimIndent()

    fun initialize() {
        try {
            // Load PLY file
            val plyLoader = PLYLoader()
            val plyData = plyLoader.loadPLY(context, "scaniverse-model 62.ply")

            if (plyData == null) {
                android.util.Log.e("PLYModel", "Failed to load PLY file - file may be missing or corrupted")
                throw RuntimeException("Failed to load PLY file: scaniverse-model 62.ply not found or invalid format")
            }

        vertexCount = plyData.vertexCount
        boundingBox = plyData.boundingBox

        // Debug logging
        android.util.Log.d("PLYModel", "Loaded PLY: vertexCount=$vertexCount")
        android.util.Log.d("PLYModel", "Bounding box: width=${boundingBox?.width}, height=${boundingBox?.height}, depth=${boundingBox?.depth}")
        android.util.Log.d("PLYModel", "Center: (${boundingBox?.centerX}, ${boundingBox?.centerY}, ${boundingBox?.centerZ})")

        // Create vertex buffer
        val vbb = ByteBuffer.allocateDirect(plyData.vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(plyData.vertices)
        vertexBuffer?.position(0)

        // Create color buffer
        val cbb = ByteBuffer.allocateDirect(plyData.colors.size * 4)
        cbb.order(ByteOrder.nativeOrder())
        colorBuffer = cbb.asFloatBuffer()
        colorBuffer?.put(plyData.colors)
        colorBuffer?.position(0)

        // Create normal buffer
        plyData.normals?.let { normals ->
            val nbb = ByteBuffer.allocateDirect(normals.size * 4)
            nbb.order(ByteOrder.nativeOrder())
            normalBuffer = nbb.asFloatBuffer()
            normalBuffer?.put(normals)
            normalBuffer?.position(0)
        }

        // Calculate model transformation matrix
        calculateModelMatrix(plyData.boundingBox)

            // Create shader program
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            // Check for linking errors
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Program linking error: $log")
            }

        // Get attribute handles
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "vColor")
        normalHandle = GLES20.glGetAttribLocation(program, "vNormal")

        // Get uniform handles
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        modelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        renderModeHandle = GLES20.glGetUniformLocation(program, "uRenderMode")

            // Get lighting uniform handles
            ambientColorHandle = GLES20.glGetUniformLocation(program, "uAmbientColor")
            lightPos1Handle = GLES20.glGetUniformLocation(program, "uLightPos1")
            lightColor1Handle = GLES20.glGetUniformLocation(program, "uLightColor1")
            lightPos2Handle = GLES20.glGetUniformLocation(program, "uLightPos2")
            lightColor2Handle = GLES20.glGetUniformLocation(program, "uLightColor2")
            lightPos3Handle = GLES20.glGetUniformLocation(program, "uLightPos3")
            lightColor3Handle = GLES20.glGetUniformLocation(program, "uLightColor3")
            viewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")

            android.util.Log.i("PLYModel", "Initialization completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("PLYModel", "Error during initialization", e)
            throw e
        }
    }

    fun draw(vpMatrix: FloatArray, cameraEyeX: Float = 0f, cameraEyeY: Float = 0f, cameraEyeZ: Float = 10f) {
        // Use shader program
        GLES20.glUseProgram(program)

        // Check for GL errors
        var error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            android.util.Log.e("PLYModel", "GL Error after glUseProgram: $error")
        }

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)

        // Set vertex data
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            0, vertexBuffer
        )

        // Set color data
        GLES20.glVertexAttribPointer(
            colorHandle, 3,
            GLES20.GL_FLOAT, false,
            0, colorBuffer
        )

        // Set normal data
        normalBuffer?.let { nBuffer ->
            GLES20.glVertexAttribPointer(
                normalHandle, 3,
                GLES20.GL_FLOAT, false,
                0, nBuffer
            )
        }

        // Compute final MVP matrix by combining VP matrix with model matrix
        val finalMVP = FloatArray(16)
        android.opengl.Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0)

        // Set MVP matrix and model matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMVP, 0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        // Set render mode uniform
        GLES20.glUniform1i(renderModeHandle, renderMode)

        // Set lighting uniforms
        setLightingUniforms(cameraEyeX, cameraEyeY, cameraEyeZ)

        // Draw based on render mode
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)

        // Check for GL errors after drawing
        error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            android.util.Log.e("PLYModel", "GL Error after glDrawArrays: $error")
        }

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    /**
     * Set up lighting uniforms for the 3-light system
     * Professional studio-quality lighting setup
     */
    private fun setLightingUniforms(cameraEyeX: Float, cameraEyeY: Float, cameraEyeZ: Float) {
        // Enhanced ambient light - brighter base illumination for better visibility
        GLES20.glUniform3f(ambientColorHandle, 0.4f, 0.42f, 0.45f)

        // Light 1 - Main key light (bright warm white from upper front)
        // Positioned to follow camera for consistent illumination
        GLES20.glUniform3f(lightPos1Handle, cameraEyeX + 2f, cameraEyeY + 4f, cameraEyeZ + 2f)
        GLES20.glUniform3f(lightColor1Handle, 1.2f, 1.15f, 1.1f)  // Brighter

        // Light 2 - Fill light (soft cool white from opposite side)
        // Reduces harsh shadows and adds depth
        GLES20.glUniform3f(lightPos2Handle, -4f, 2f, 3f)
        GLES20.glUniform3f(lightColor2Handle, 0.7f, 0.75f, 0.85f)

        // Light 3 - Rim/back light (subtle blue for depth and definition)
        // Creates separation from background
        GLES20.glUniform3f(lightPos3Handle, 3f, 3f, -5f)
        GLES20.glUniform3f(lightColor3Handle, 0.5f, 0.6f, 0.8f)

        // View position (camera position for specular highlights)
        GLES20.glUniform3f(viewPosHandle, cameraEyeX, cameraEyeY, cameraEyeZ)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // Check for compilation errors
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation error: $log")
        }

        return shader
    }

    /**
     * Calculate model transformation matrix to center PLY model at origin
     * Uses uniform scaling to preserve natural proportions
     * Model is transformed: translate to origin, then scale uniformly
     */
    private fun calculateModelMatrix(boundingBox: PLYLoader.BoundingBox) {
        val modelMat = FloatArray(16)
        android.opengl.Matrix.setIdentityM(modelMat, 0)

        // Calculate the center of the bounding box in original coordinates
        val centerX = boundingBox.centerX
        val centerY = boundingBox.centerY
        val centerZ = boundingBox.centerZ

        // Calculate scale factors for each axis
        val scaleX = targetWidth / boundingBox.width
        val scaleY = targetHeight / boundingBox.height
        val scaleZ = targetDepth / boundingBox.depth

        // Use MINIMUM scale to preserve proportions and keep model at natural size
        // This prevents the model from being squashed or stretched
        val scale = minOf(scaleX, scaleY, scaleZ)

        android.util.Log.d("PLYModel", "=== PLY Model Transformation ===")
        android.util.Log.d("PLYModel", "Original bounding box:")
        android.util.Log.d("PLYModel", "  Size: ${boundingBox.width} x ${boundingBox.height} x ${boundingBox.depth}")
        android.util.Log.d("PLYModel", "  Center: ($centerX, $centerY, $centerZ)")
        android.util.Log.d("PLYModel", "  Min: (${boundingBox.minX}, ${boundingBox.minY}, ${boundingBox.minZ})")
        android.util.Log.d("PLYModel", "  Max: (${boundingBox.maxX}, ${boundingBox.maxY}, ${boundingBox.maxZ})")
        android.util.Log.d("PLYModel", "Target room: $targetWidth x $targetHeight x $targetDepth")
        android.util.Log.d("PLYModel", "Scale factors - X:$scaleX Y:$scaleY Z:$scaleZ")
        android.util.Log.d("PLYModel", "Using MINIMUM uniform scale: $scale to preserve proportions")

        // Apply transformations in correct order:
        // 1. First scale the model uniformly
        android.opengl.Matrix.scaleM(modelMat, 0, scale, scale, scale)

        // 2. Then translate to center at origin (0,0,0)
        // Important: Translation happens AFTER scaling, so we translate the scaled center
        android.opengl.Matrix.translateM(modelMat, 0, -centerX, -centerY, -centerZ)

        // Calculate actual dimensions after scaling
        actualWidth = boundingBox.width * scale
        actualHeight = boundingBox.height * scale
        actualDepth = boundingBox.depth * scale

        android.util.Log.d("PLYModel", "Final model size: $actualWidth x $actualHeight x $actualDepth")
        android.util.Log.d("PLYModel", "Model centered at (0, 0, 0) for proper pivot")
        android.util.Log.d("PLYModel", "Scaled model bounds: X: ±${actualWidth/2}, Y: ±${actualHeight/2}, Z: ±${actualDepth/2}")

        // Store the model matrix
        System.arraycopy(modelMat, 0, modelMatrix, 0, 16)
    }

    // Get actual model dimensions (after transformation)
    fun getModelWidth(): Float = actualWidth
    fun getModelHeight(): Float = actualHeight
    fun getModelDepth(): Float = actualDepth

    companion object {
        // Room dimensions (updated to match the new larger PLY model dimensions)
        const val ROOM_WIDTH = 9f
        const val ROOM_HEIGHT = 6f
        const val ROOM_DEPTH = 12f

        // Helper functions for other classes that need room dimensions
        fun getRoomWidth(): Float = ROOM_WIDTH
        fun getRoomHeight(): Float = ROOM_HEIGHT
        fun getRoomDepth(): Float = ROOM_DEPTH

        // Floor Y coordinate (bottom of the room)
        fun getFloorY(): Float = -ROOM_HEIGHT / 2
    }
}
