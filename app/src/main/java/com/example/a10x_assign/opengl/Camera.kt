package com.example.a10x_assign.opengl

import android.opengl.Matrix
import javax.inject.Inject

class Camera @Inject constructor() {
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    // Camera position
    var eyeX = 0f
    var eyeY = 2f
    var eyeZ = 12f

    // Look at point (pan target) - center of PLY model after transformation
    var centerX = 0f
    var centerY = 0f  // PLY model is centered at origin
    var centerZ = 0f

    // Up vector
    private val upX = 0f
    private val upY = 1f
    private val upZ = 0f

    // Rotation angles - start with front view, not top view
    // angleY = 0 means looking at door side (front)
    // angleX controls pitch - start at 0 for front view (was causing top view)
    var angleX = 0f   // Pitch: 0 = horizontal (front view), positive = look down, negative = look up
    var angleY = 0f   // Yaw: 0 = door side (front), 90 = right side, -90 = left side, 180 = back

    // Zoom - increased range to allow getting inside
    var zoom = 1f
        set(value) {
            field = value.coerceIn(0.3f, 5.0f)  // Wider range: 0.3x (far) to 5x (very close/inside)
        }

    // Distance from center - increased for larger PLY model
    private var distance = 15f

    // Pan limits - REMOVED for completely free movement
    // No restrictions, can pan anywhere
    private val panLimitX = Float.MAX_VALUE
    private val panLimitY = Float.MAX_VALUE
    private val panLimitZ = Float.MAX_VALUE

    init {
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
    }

    fun setProjection(width: Int, height: Int) {
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 1f, 50f)
    }

    fun updateViewMatrix() {
        // Apply zoom to distance
        val actualDistance = distance / zoom

        // Calculate camera position based on rotation angles
        // Camera orbits around the center point (centerX, centerY, centerZ)
        // angleY = 0 means looking from the door side (front, positive Z direction)
        // Rotation is relative to door side as front reference

        val pitchRad = Math.toRadians(angleX.toDouble())
        val yawRad = Math.toRadians(angleY.toDouble())

        // Position camera in spherical coordinates relative to center
        // For angleY=0, camera is at positive Z (door side/front)
        eyeX = centerX + actualDistance * Math.sin(yawRad).toFloat() * Math.cos(pitchRad).toFloat()
        eyeY = centerY + actualDistance * Math.sin(pitchRad).toFloat()
        eyeZ = centerZ + actualDistance * Math.cos(yawRad).toFloat() * Math.cos(pitchRad).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            centerX, centerY, centerZ,
            upX, upY, upZ
        )
    }

    fun getVPMatrix(): FloatArray {
        updateViewMatrix()
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        return vpMatrix
    }

    fun rotate(deltaX: Float, deltaY: Float) {
        angleY += deltaX * 0.5f
        angleX += deltaY * 0.5f

        // Clamp vertical rotation to prevent flipping
        angleX = angleX.coerceIn(-89f, 89f)
    }

    fun adjustZoom(scaleFactor: Float) {
        zoom *= scaleFactor
        zoom = zoom.coerceIn(0.3f, 5.0f)  // Match the zoom property limits
    }

    fun pan(deltaX: Float, deltaY: Float) {
        // Two-finger pan: Move the look-at point (translate view)
        // This allows moving the room left/right/forward/back

        // Calculate camera-relative right and forward vectors
        val radY = Math.toRadians(angleY.toDouble())

        // Right vector (perpendicular to view direction on horizontal plane)
        val rightX = -Math.cos(radY).toFloat()
        val rightZ = Math.sin(radY).toFloat()

        // Forward vector on horizontal plane
        val forwardX = Math.sin(radY).toFloat()
        val forwardZ = Math.cos(radY).toFloat()

        // Pan sensitivity
        val panSpeed = 0.02f

        // Move the look-at center point
        // deltaX moves left/right, deltaY moves forward/back
        centerX += (rightX * deltaX + forwardX * deltaY) * panSpeed
        centerZ += (rightZ * deltaX + forwardZ * deltaY) * panSpeed

        // Optionally add vertical movement with deltaY
        // centerY += -deltaY * panSpeed * 0.5f  // Uncomment for up/down panning

        // No limits - free movement
    }

    // Check if camera is inside the room
    // Room dimensions: width=9, height=6, depth=12 (updated to match PLYModel)
    fun isInsideRoom(): Boolean {
        val roomWidth = PLYModel.ROOM_WIDTH
        val roomHeight = PLYModel.ROOM_HEIGHT
        val roomDepth = PLYModel.ROOM_DEPTH

        val halfWidth = roomWidth / 2
        val halfHeight = roomHeight / 2
        val halfDepth = roomDepth / 2

        return eyeX >= -halfWidth && eyeX <= halfWidth &&
               eyeY >= -halfHeight && eyeY <= halfHeight &&
               eyeZ >= -halfDepth && eyeZ <= halfDepth
    }

    // Reset camera to face door side (front) directly
    fun resetToCenter() {
        centerX = 0f
        centerY = 0f  // PLY model center
        centerZ = 0f
        angleX = 0f   // Look horizontally
        angleY = 0f   // Face door side (front)
        zoom = 1f
    }
}