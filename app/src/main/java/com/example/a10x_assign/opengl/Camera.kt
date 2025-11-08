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

    // Look at point (pan target)
    var centerX = 0f
    var centerY = 0.5f
    var centerZ = 0f

    // Up vector
    private val upX = 0f
    private val upY = 1f
    private val upZ = 0f

    // Rotation angles
    var angleX = 15f  // Start with slight downward angle
    var angleY = 25f  // Start with slight rotation

    // Zoom
    var zoom = 1f
        set(value) {
            field = value.coerceIn(0.5f, 3.5f)
        }

    // Distance from center
    private var distance = 12f

    // Pan limits
    private val panLimitX = 5f
    private val panLimitY = 3f
    private val panLimitZ = 6f

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
        eyeX = actualDistance * Math.sin(Math.toRadians(angleY.toDouble())).toFloat() *
               Math.cos(Math.toRadians(angleX.toDouble())).toFloat()
        eyeY = actualDistance * Math.sin(Math.toRadians(angleX.toDouble())).toFloat()
        eyeZ = actualDistance * Math.cos(Math.toRadians(angleY.toDouble())).toFloat() *
               Math.cos(Math.toRadians(angleX.toDouble())).toFloat()

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
        zoom = zoom.coerceIn(0.5f, 3.5f)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        // Calculate the right and up vectors based on current camera orientation
        val radY = Math.toRadians(angleY.toDouble())
        val radX = Math.toRadians(angleX.toDouble())

        // Right vector (perpendicular to view direction on horizontal plane)
        val rightX = -Math.cos(radY).toFloat()
        val rightZ = Math.sin(radY).toFloat()

        // Up vector (adjusted for camera tilt)
        val upX = Math.sin(radY).toFloat() * Math.sin(radX).toFloat()
        val upY = Math.cos(radX).toFloat()
        val upZ = Math.cos(radY).toFloat() * Math.sin(radX).toFloat()

        // Pan sensitivity
        val panSpeed = 0.01f

        // Update center position
        centerX += (rightX * deltaX + upX * deltaY) * panSpeed
        centerY += upY * deltaY * panSpeed
        centerZ += (rightZ * deltaX + upZ * deltaY) * panSpeed

        // Apply limits to prevent panning too far
        centerX = centerX.coerceIn(-panLimitX, panLimitX)
        centerY = centerY.coerceIn(-panLimitY, panLimitY)
        centerZ = centerZ.coerceIn(-panLimitZ, panLimitZ)
    }

    // Check if camera is inside the room
    // Room dimensions: width=6, height=4, depth=8 (from Room.kt)
    fun isInsideRoom(): Boolean {
        val roomWidth = 6f
        val roomHeight = 4f
        val roomDepth = 8f

        val halfWidth = roomWidth / 2
        val halfHeight = roomHeight / 2
        val halfDepth = roomDepth / 2

        return eyeX >= -halfWidth && eyeX <= halfWidth &&
               eyeY >= -halfHeight && eyeY <= halfHeight &&
               eyeZ >= -halfDepth && eyeZ <= halfDepth
    }
}