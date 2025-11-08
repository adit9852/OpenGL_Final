package com.example.a10x_assign.opengl

import android.opengl.Matrix
import com.example.a10x_assign.data.WallType
import javax.inject.Inject

data class RayHit(
    val wallType: WallType,
    val x: Float,  // Normalized 0-1 coordinate on wall
    val y: Float,  // Normalized 0-1 coordinate on wall
    val distance: Float,  // Distance from camera (for sorting)
    val worldX: Float = 0f,  // Actual world X coordinate
    val worldY: Float = 0f,  // Actual world Y coordinate
    val worldZ: Float = 0f   // Actual world Z coordinate
)

class RayCaster @Inject constructor() {
    // Room dimensions (must match Room.kt)
    private val width = 6f
    private val height = 4f
    private val depth = 8f

    /**
     * Cast a ray from screen coordinates into the 3D world and find wall intersections
     * @param screenX Screen X coordinate (pixels)
     * @param screenY Screen Y coordinate (pixels)
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @param camera The camera object with view/projection matrices
     * @return The nearest wall hit, or null if no hit
     */
    fun castRay(
        screenX: Float,
        screenY: Float,
        screenWidth: Int,
        screenHeight: Int,
        camera: Camera
    ): RayHit? {
        // Convert screen coordinates to normalized device coordinates (-1 to 1)
        val x = (2.0f * screenX) / screenWidth - 1.0f
        val y = 1.0f - (2.0f * screenY) / screenHeight  // Flip Y

        // Get view and projection matrices
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)

        // Recreate the matrices from camera
        Matrix.setLookAtM(
            viewMatrix, 0,
            camera.eyeX, camera.eyeY, camera.eyeZ,
            camera.centerX, camera.centerY, camera.centerZ,
            0f, 1f, 0f
        )

        // Get projection matrix (recreate with same parameters)
        val ratio = screenWidth.toFloat() / screenHeight.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 1f, 50f)

        // Create inverse matrices
        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val invertedVPMatrix = FloatArray(16)
        if (!Matrix.invertM(invertedVPMatrix, 0, vpMatrix, 0)) {
            return null  // Matrix inversion failed
        }

        // Near and far points in normalized device coordinates
        val nearPoint = floatArrayOf(x, y, -1f, 1f)  // Near plane
        val farPoint = floatArrayOf(x, y, 1f, 1f)    // Far plane

        // Transform to world space
        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)

        Matrix.multiplyMV(nearWorld, 0, invertedVPMatrix, 0, nearPoint, 0)
        Matrix.multiplyMV(farWorld, 0, invertedVPMatrix, 0, farPoint, 0)

        // Perspective division
        nearWorld[0] /= nearWorld[3]
        nearWorld[1] /= nearWorld[3]
        nearWorld[2] /= nearWorld[3]

        farWorld[0] /= farWorld[3]
        farWorld[1] /= farWorld[3]
        farWorld[2] /= farWorld[3]

        // Ray origin and direction
        val rayOrigin = floatArrayOf(nearWorld[0], nearWorld[1], nearWorld[2])
        val rayDirection = floatArrayOf(
            farWorld[0] - nearWorld[0],
            farWorld[1] - nearWorld[1],
            farWorld[2] - nearWorld[2]
        )

        // Normalize direction
        val length = Math.sqrt(
            (rayDirection[0] * rayDirection[0] +
             rayDirection[1] * rayDirection[1] +
             rayDirection[2] * rayDirection[2]).toDouble()
        ).toFloat()

        rayDirection[0] /= length
        rayDirection[1] /= length
        rayDirection[2] /= length

        // Test intersection with all walls and find the closest one
        val hits = mutableListOf<RayHit>()

        // Test each wall
        testBackWall(rayOrigin, rayDirection)?.let { hits.add(it) }
        testFrontWall(rayOrigin, rayDirection)?.let { hits.add(it) }
        testLeftWall(rayOrigin, rayDirection)?.let { hits.add(it) }
        testRightWall(rayOrigin, rayDirection)?.let { hits.add(it) }
        testFloor(rayOrigin, rayDirection)?.let { hits.add(it) }
        testCeiling(rayOrigin, rayDirection)?.let { hits.add(it) }

        // Return the closest hit
        return hits.minByOrNull { it.distance }
    }

    private fun testBackWall(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        // Back wall is at z = -d, normal = (0, 0, 1)
        val planeZ = -d
        if (Math.abs(direction[2]) < 0.0001f) return null  // Ray parallel to plane

        val t = (planeZ - origin[2]) / direction[2]
        if (t < 0) return null  // Behind camera

        val hitX = origin[0] + direction[0] * t
        val hitY = origin[1] + direction[1] * t

        // Check if hit is within wall bounds
        if (hitX < -w || hitX > w || hitY < -h || hitY > h) return null

        // Convert to normalized coordinates (0-1)
        val normalizedX = (hitX + w) / width
        val normalizedY = (hitY + h) / height

        return RayHit(WallType.BACK_WALL, normalizedX, normalizedY, t, hitX, planeZ, hitY)
    }

    private fun testFrontWall(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val planeZ = d
        if (Math.abs(direction[2]) < 0.0001f) return null

        val t = (planeZ - origin[2]) / direction[2]
        if (t < 0) return null

        val hitX = origin[0] + direction[0] * t
        val hitY = origin[1] + direction[1] * t

        if (hitX < -w || hitX > w || hitY < -h || hitY > h) return null

        val normalizedX = (hitX + w) / width
        val normalizedY = (hitY + h) / height

        return RayHit(WallType.FRONT_WALL, normalizedX, normalizedY, t, hitX, planeZ, hitY)
    }

    private fun testLeftWall(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val planeX = -w
        if (Math.abs(direction[0]) < 0.0001f) return null

        val t = (planeX - origin[0]) / direction[0]
        if (t < 0) return null

        val hitZ = origin[2] + direction[2] * t
        val hitY = origin[1] + direction[1] * t

        if (hitZ < -d || hitZ > d || hitY < -h || hitY > h) return null

        val normalizedX = (hitZ + d) / depth
        val normalizedY = (hitY + h) / height

        return RayHit(WallType.LEFT_WALL, normalizedX, normalizedY, t, planeX, hitY, hitZ)
    }

    private fun testRightWall(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val planeX = w
        if (Math.abs(direction[0]) < 0.0001f) return null

        val t = (planeX - origin[0]) / direction[0]
        if (t < 0) return null

        val hitZ = origin[2] + direction[2] * t
        val hitY = origin[1] + direction[1] * t

        if (hitZ < -d || hitZ > d || hitY < -h || hitY > h) return null

        val normalizedX = (hitZ + d) / depth
        val normalizedY = (hitY + h) / height

        return RayHit(WallType.RIGHT_WALL, normalizedX, normalizedY, t, planeX, hitY, hitZ)
    }

    private fun testFloor(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val planeY = -h
        if (Math.abs(direction[1]) < 0.0001f) return null

        val t = (planeY - origin[1]) / direction[1]
        if (t < 0) return null

        val hitX = origin[0] + direction[0] * t
        val hitZ = origin[2] + direction[2] * t

        if (hitX < -w || hitX > w || hitZ < -d || hitZ > d) return null

        val normalizedX = (hitX + w) / width
        val normalizedY = (hitZ + d) / depth

        return RayHit(WallType.FLOOR, normalizedX, normalizedY, t, hitX, planeY, hitZ)
    }

    private fun testCeiling(origin: FloatArray, direction: FloatArray): RayHit? {
        val w = width / 2
        val h = height / 2
        val d = depth / 2

        val planeY = h
        if (Math.abs(direction[1]) < 0.0001f) return null

        val t = (planeY - origin[1]) / direction[1]
        if (t < 0) return null

        val hitX = origin[0] + direction[0] * t
        val hitZ = origin[2] + direction[2] * t

        if (hitX < -w || hitX > w || hitZ < -d || hitZ > d) return null

        val normalizedX = (hitX + w) / width
        val normalizedY = (hitZ + d) / depth

        return RayHit(WallType.CEILING, normalizedX, normalizedY, t, hitX, planeY, hitZ)
    }
}
