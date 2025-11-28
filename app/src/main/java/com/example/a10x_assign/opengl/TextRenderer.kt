package com.example.a10x_assign.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.a10x_assign.data.WallType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.inject.Inject

class TextRenderer @Inject constructor() {
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D uTexture;
        uniform vec4 uColor;
        varying vec2 vTexCoord;
        void main() {
            vec4 texColor = texture2D(uTexture, vTexCoord);
            gl_FragColor = vec4(uColor.rgb, texColor.a * uColor.a);
        }
    """.trimIndent()

    private var program: Int = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var textureHandle: Int = 0

    private var isInitialized = false

    // Texture IDs for different labels
    private val textureIds = mutableMapOf<String, Int>()

    // Room dimensions (must match PLYModel.kt)
    private val width = PLYModel.ROOM_WIDTH   // 9f
    private val height = PLYModel.ROOM_HEIGHT  // 6f
    private val depth = PLYModel.ROOM_DEPTH    // 12f

    private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    fun initialize(context: Context) {
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

        // Initialize index buffer
        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)

        // Initialize texture coordinates buffer (flipped vertically for correct orientation)
        val texCoords = floatArrayOf(
            0f, 1f,  // bottom left -> top left in texture
            1f, 1f,  // bottom right -> top right in texture
            1f, 0f,  // top right -> bottom right in texture
            0f, 0f   // top left -> bottom left in texture
        )
        val tcb = ByteBuffer.allocateDirect(texCoords.size * 4)
        tcb.order(ByteOrder.nativeOrder())
        texCoordBuffer = tcb.asFloatBuffer()
        texCoordBuffer.put(texCoords)
        texCoordBuffer.position(0)

        // Clear old textures if any
        textureIds.clear()

        // Create textures for wall labels
        createTextTexture(context, "BACK WALL", "back_wall")
        createTextTexture(context, "FRONT WALL", "front_wall")
        createTextTexture(context, "LEFT WALL", "left_wall")
        createTextTexture(context, "RIGHT WALL", "right_wall")
        createTextTexture(context, "FLOOR", "floor")
        createTextTexture(context, "CEILING", "ceiling")

        // Create textures for annotation labels
        createTextTexture(context, "SPRAY AREA", "spray_area", android.graphics.Color.WHITE)
        createTextTexture(context, "SAND AREA", "sand_area", android.graphics.Color.BLACK)
        createTextTexture(context, "OBSTACLE", "obstacle", android.graphics.Color.WHITE)

        isInitialized = true
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun createTextTexture(context: Context, text: String, key: String, textColor: Int = android.graphics.Color.WHITE) {
        val textSize = 64f
        val paint = Paint().apply {
            this.textSize = textSize
            this.color = textColor
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val textWidth = paint.measureText(text)
        val textHeight = textSize * 1.5f

        val bitmap = Bitmap.createBitmap(textWidth.toInt(), textHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, textWidth / 2f, textSize, paint)

        // Generate texture
        val textureHandles = IntArray(1)
        GLES20.glGenTextures(1, textureHandles, 0)

        val textureId = textureHandles[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        bitmap.recycle()

        textureIds[key] = textureId
    }

    fun drawWallLabels(vpMatrix: FloatArray) {
        if (!isInitialized) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        // Set white color for wall labels
        GLES20.glUniform4f(colorHandle, 1f, 1f, 1f, 1f)

        // Draw label on each wall
        drawLabelOnWall(vpMatrix, WallType.BACK_WALL, "back_wall")
        drawLabelOnWall(vpMatrix, WallType.FRONT_WALL, "front_wall")
        drawLabelOnWall(vpMatrix, WallType.LEFT_WALL, "left_wall")
        drawLabelOnWall(vpMatrix, WallType.RIGHT_WALL, "right_wall")
        drawLabelOnWall(vpMatrix, WallType.FLOOR, "floor")
        drawLabelOnWall(vpMatrix, WallType.CEILING, "ceiling")

        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    fun drawAnnotationLabel(vpMatrix: FloatArray, annotation: com.example.a10x_assign.data.AnnotationEntity) {
        if (!isInitialized) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        val labelKey = when (annotation.type) {
            com.example.a10x_assign.data.AnnotationType.SPRAY_AREA -> "spray_area"
            com.example.a10x_assign.data.AnnotationType.SAND_AREA -> "sand_area"
            com.example.a10x_assign.data.AnnotationType.OBSTACLE -> "obstacle"
        }

        // Set appropriate color for text
        val color = when (annotation.type) {
            com.example.a10x_assign.data.AnnotationType.SPRAY_AREA -> floatArrayOf(1f, 1f, 1f, 1f)  // White on red
            com.example.a10x_assign.data.AnnotationType.SAND_AREA -> floatArrayOf(0f, 0f, 0f, 1f)   // Black on yellow
            com.example.a10x_assign.data.AnnotationType.OBSTACLE -> floatArrayOf(1f, 1f, 1f, 1f)    // White on orange
        }

        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        drawTextOnAnnotation(vpMatrix, annotation, labelKey)

        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun drawLabelOnWall(vpMatrix: FloatArray, wallType: WallType, textureKey: String) {
        val textureId = textureIds[textureKey] ?: return

        val w = width / 2
        val h = height / 2
        val d = depth / 2

        // Label size (smaller than wall)
        val labelWidth = 1.5f
        val labelHeight = 0.4f

        // Position label at center of each wall
        val vertices = when (wallType) {
            WallType.BACK_WALL -> floatArrayOf(
                -labelWidth/2, -labelHeight/2, -d + 0.02f,
                labelWidth/2, -labelHeight/2, -d + 0.02f,
                labelWidth/2, labelHeight/2, -d + 0.02f,
                -labelWidth/2, labelHeight/2, -d + 0.02f
            )
            WallType.FRONT_WALL -> floatArrayOf(
                -labelWidth/2, -labelHeight/2, d - 0.02f,
                labelWidth/2, -labelHeight/2, d - 0.02f,
                labelWidth/2, labelHeight/2, d - 0.02f,
                -labelWidth/2, labelHeight/2, d - 0.02f
            )
            WallType.LEFT_WALL -> floatArrayOf(
                -w + 0.02f, -labelHeight/2, -labelWidth/2,
                -w + 0.02f, -labelHeight/2, labelWidth/2,
                -w + 0.02f, labelHeight/2, labelWidth/2,
                -w + 0.02f, labelHeight/2, -labelWidth/2
            )
            WallType.RIGHT_WALL -> floatArrayOf(
                w - 0.02f, -labelHeight/2, -labelWidth/2,
                w - 0.02f, -labelHeight/2, labelWidth/2,
                w - 0.02f, labelHeight/2, labelWidth/2,
                w - 0.02f, labelHeight/2, -labelWidth/2
            )
            WallType.FLOOR -> floatArrayOf(
                -labelWidth/2, -h + 0.02f, -labelHeight/2,
                labelWidth/2, -h + 0.02f, -labelHeight/2,
                labelWidth/2, -h + 0.02f, labelHeight/2,
                -labelWidth/2, -h + 0.02f, labelHeight/2
            )
            WallType.CEILING -> floatArrayOf(
                -labelWidth/2, h - 0.02f, -labelHeight/2,
                labelWidth/2, h - 0.02f, -labelHeight/2,
                labelWidth/2, h - 0.02f, labelHeight/2,
                -labelWidth/2, h - 0.02f, labelHeight/2
            )
        }

        // Flip texture horizontally for LEFT_WALL and FRONT_WALL
        val flipHorizontally = (wallType == WallType.LEFT_WALL || wallType == WallType.FRONT_WALL)
        drawTexturedQuad(vpMatrix, vertices, textureId, flipHorizontally)
    }

    private fun drawTextOnAnnotation(vpMatrix: FloatArray, annotation: com.example.a10x_assign.data.AnnotationEntity, textureKey: String) {
        val textureId = textureIds[textureKey] ?: return

        val w = width / 2
        val h = height / 2
        val d = depth / 2

        // Calculate position on wall (center of annotation)
        val centerX = annotation.x + annotation.width / 2f
        val centerY = annotation.y + annotation.height / 2f

        // Label size (smaller than annotation)
        val labelWidth = annotation.width * width * 0.8f
        val labelHeight = annotation.height * height * 0.3f

        val vertices = when (annotation.wallType) {
            WallType.BACK_WALL -> {
                val worldX = -w + (centerX * width)
                val worldY = -h + (centerY * height)
                floatArrayOf(
                    worldX - labelWidth/2, worldY - labelHeight/2, -d + 0.03f,
                    worldX + labelWidth/2, worldY - labelHeight/2, -d + 0.03f,
                    worldX + labelWidth/2, worldY + labelHeight/2, -d + 0.03f,
                    worldX - labelWidth/2, worldY + labelHeight/2, -d + 0.03f
                )
            }
            WallType.FRONT_WALL -> {
                val worldX = -w + (centerX * width)
                val worldY = -h + (centerY * height)
                floatArrayOf(
                    worldX - labelWidth/2, worldY - labelHeight/2, d - 0.03f,
                    worldX + labelWidth/2, worldY - labelHeight/2, d - 0.03f,
                    worldX + labelWidth/2, worldY + labelHeight/2, d - 0.03f,
                    worldX - labelWidth/2, worldY + labelHeight/2, d - 0.03f
                )
            }
            WallType.LEFT_WALL -> {
                val worldZ = -d + (centerX * depth)
                val worldY = -h + (centerY * height)
                floatArrayOf(
                    -w + 0.03f, worldY - labelHeight/2, worldZ - labelWidth/2,
                    -w + 0.03f, worldY - labelHeight/2, worldZ + labelWidth/2,
                    -w + 0.03f, worldY + labelHeight/2, worldZ + labelWidth/2,
                    -w + 0.03f, worldY + labelHeight/2, worldZ - labelWidth/2
                )
            }
            WallType.RIGHT_WALL -> {
                val worldZ = -d + (centerX * depth)
                val worldY = -h + (centerY * height)
                floatArrayOf(
                    w - 0.03f, worldY - labelHeight/2, worldZ - labelWidth/2,
                    w - 0.03f, worldY - labelHeight/2, worldZ + labelWidth/2,
                    w - 0.03f, worldY + labelHeight/2, worldZ + labelWidth/2,
                    w - 0.03f, worldY + labelHeight/2, worldZ - labelWidth/2
                )
            }
            WallType.FLOOR -> {
                val worldX = -w + (centerX * width)
                val worldZ = -d + (centerY * depth)
                floatArrayOf(
                    worldX - labelWidth/2, -h + 0.03f, worldZ - labelHeight/2,
                    worldX + labelWidth/2, -h + 0.03f, worldZ - labelHeight/2,
                    worldX + labelWidth/2, -h + 0.03f, worldZ + labelHeight/2,
                    worldX - labelWidth/2, -h + 0.03f, worldZ + labelHeight/2
                )
            }
            WallType.CEILING -> {
                val worldX = -w + (centerX * width)
                val worldZ = -d + (centerY * depth)
                floatArrayOf(
                    worldX - labelWidth/2, h - 0.03f, worldZ - labelHeight/2,
                    worldX + labelWidth/2, h - 0.03f, worldZ - labelHeight/2,
                    worldX + labelWidth/2, h - 0.03f, worldZ + labelHeight/2,
                    worldX - labelWidth/2, h - 0.03f, worldZ + labelHeight/2
                )
            }
        }

        // Flip texture horizontally for LEFT_WALL and FRONT_WALL
        val flipHorizontally = (annotation.wallType == WallType.LEFT_WALL || annotation.wallType == WallType.FRONT_WALL)
        drawTexturedQuad(vpMatrix, vertices, textureId, flipHorizontally)
    }

    private fun drawTexturedQuad(vpMatrix: FloatArray, vertices: FloatArray, textureId: Int, flipHorizontally: Boolean = false) {
        // Create vertex buffer
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Create texture coordinate buffer with optional horizontal flip
        val texCoords = if (flipHorizontally) {
            floatArrayOf(
                1f, 1f,  // bottom left -> flipped
                0f, 1f,  // bottom right -> flipped
                0f, 0f,  // top right -> flipped
                1f, 0f   // top left -> flipped
            )
        } else {
            floatArrayOf(
                0f, 1f,  // bottom left
                1f, 1f,  // bottom right
                1f, 0f,  // top right
                0f, 0f   // top left
            )
        }

        val tcb = ByteBuffer.allocateDirect(texCoords.size * 4)
        tcb.order(ByteOrder.nativeOrder())
        val localTexCoordBuffer = tcb.asFloatBuffer()
        localTexCoordBuffer.put(texCoords)
        localTexCoordBuffer.position(0)

        // Set identity model matrix
        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, localTexCoordBuffer)

        // Set texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
}
