package com.example.a10x_assign.opengl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.a10x_assign.data.AnnotationEntity
import com.example.a10x_assign.data.RobotEntity
import javax.inject.Inject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RoomRenderer @Inject constructor(
    val camera: Camera,
    val room: Room,
    private val robotCube: RobotCube,
    private val annotationOverlay: AnnotationOverlay,
    private val textRenderer: TextRenderer
) : GLSurfaceView.Renderer {

    @Volatile
    var robotPosition: RobotEntity? = null

    @Volatile
    var annotations: List<AnnotationEntity> = emptyList()

    private var context: android.content.Context? = null

    // Store viewport dimensions to restore after context recreation
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0

    // Callback for camera position changes
    var onCameraPositionChanged: ((Boolean) -> Unit)? = null

    fun setContext(context: android.content.Context) {
        this.context = context
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set the background color to a pleasant gradient-like sky color
        GLES20.glClearColor(0.78f, 0.85f, 0.92f, 1.0f)

        // Enable depth testing for proper 3D rendering
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        // Disable face culling so we can see walls from inside the room
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // Initialize the room after OpenGL context is created
        room.initialize()
        robotCube.initialize()
        annotationOverlay.initialize()

        // Initialize text renderer with context
        context?.let {
            textRenderer.initialize(it)
        }

        // Restore viewport and projection if we have valid dimensions
        // This handles the case where onSurfaceChanged might not be called
        if (viewportWidth > 0 && viewportHeight > 0) {
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
            camera.setProjection(viewportWidth, viewportHeight)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Store dimensions for context recreation
        viewportWidth = width
        viewportHeight = height

        GLES20.glViewport(0, 0, width, height)
        camera.setProjection(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear the color and depth buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Get the combined projection and view matrix
        val vpMatrix = camera.getVPMatrix()

        // Check if camera is inside room and notify
        onCameraPositionChanged?.invoke(camera.isInsideRoom())

        // Draw the room
        room.draw(vpMatrix)

        // Draw wall labels
        textRenderer.drawWallLabels(vpMatrix)

        // Draw annotations on walls
        annotationOverlay.drawAnnotations(vpMatrix, annotations)

        // Draw annotation labels on top of each annotation
        annotations.forEach { annotation ->
            textRenderer.drawAnnotationLabel(vpMatrix, annotation)
        }

        // Draw the robot if placed
        robotPosition?.let { robot ->
            robotCube.draw(vpMatrix, robot.x, robot.y, robot.z, robot.rotationY)
        }
    }
}
