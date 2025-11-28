package com.example.a10x_assign.opengl

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PLY file loader for binary little-endian format
 */
class PLYLoader {

    data class BoundingBox(
        var minX: Float = Float.POSITIVE_INFINITY,
        var maxX: Float = Float.NEGATIVE_INFINITY,
        var minY: Float = Float.POSITIVE_INFINITY,
        var maxY: Float = Float.NEGATIVE_INFINITY,
        var minZ: Float = Float.POSITIVE_INFINITY,
        var maxZ: Float = Float.NEGATIVE_INFINITY
    ) {
        val width: Float get() = maxX - minX
        val height: Float get() = maxY - minY
        val depth: Float get() = maxZ - minZ
        val centerX: Float get() = (minX + maxX) / 2f
        val centerY: Float get() = (minY + maxY) / 2f
        val centerZ: Float get() = (minZ + maxZ) / 2f
    }

    data class PLYData(
        val vertices: FloatArray,
        val colors: FloatArray,
        val normals: FloatArray?,
        val vertexCount: Int,
        val boundingBox: BoundingBox
    )

    fun loadPLY(context: Context, fileName: String): PLYData? {
        try {
            val inputStream = context.assets.open(fileName)
            val bufferedStream = BufferedInputStream(inputStream)

            // Read header
            val header = readHeader(bufferedStream)
            val vertexCount = header.first
            val format = header.second

            if (format != "binary_little_endian") {
                throw IllegalArgumentException("Only binary_little_endian format is supported")
            }

            // Read binary vertex data
            val dataStream = DataInputStream(bufferedStream)
            val vertices = FloatArray(vertexCount * 3) // x, y, z
            val colors = FloatArray(vertexCount * 3) // r, g, b
            val boundingBox = BoundingBox()

            for (i in 0 until vertexCount) {
                // Read position (3 floats)
                val x = readLittleEndianFloat(dataStream)
                val y = readLittleEndianFloat(dataStream)
                val z = readLittleEndianFloat(dataStream)

                // Apply rotation to fix orientation: rotate 90° around X-axis
                // This transforms the coordinate system from top-view to front-view
                // X stays X, Y -> -Z, Z -> Y
                val rotatedX = x
                val rotatedY = z      // Old Z becomes new Y (up/down)
                val rotatedZ = -y     // Old Y becomes new -Z (depth)

                vertices[i * 3] = rotatedX
                vertices[i * 3 + 1] = rotatedY
                vertices[i * 3 + 2] = rotatedZ

                // Update bounding box with rotated coordinates
                if (rotatedX < boundingBox.minX) boundingBox.minX = rotatedX
                if (rotatedX > boundingBox.maxX) boundingBox.maxX = rotatedX
                if (rotatedY < boundingBox.minY) boundingBox.minY = rotatedY
                if (rotatedY > boundingBox.maxY) boundingBox.maxY = rotatedY
                if (rotatedZ < boundingBox.minZ) boundingBox.minZ = rotatedZ
                if (rotatedZ > boundingBox.maxZ) boundingBox.maxZ = rotatedZ

                // Read color (3 unsigned bytes)
                val r = dataStream.readUnsignedByte()
                val g = dataStream.readUnsignedByte()
                val b = dataStream.readUnsignedByte()

                // Convert from 0-255 to 0.0-1.0
                colors[i * 3] = r / 255f
                colors[i * 3 + 1] = g / 255f
                colors[i * 3 + 2] = b / 255f
            }

            dataStream.close()
            bufferedStream.close()
            inputStream.close()

            // Generate normals for point cloud lighting
            val normals = generateNormals(vertices, vertexCount, boundingBox)

            return PLYData(vertices, colors, normals, vertexCount, boundingBox)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun readHeader(stream: BufferedInputStream): Pair<Int, String> {
        var vertexCount = 0
        var format = ""
        val headerLines = mutableListOf<String>()

        var currentLine = StringBuilder()
        var byte: Int

        while (true) {
            byte = stream.read()
            if (byte == -1) break

            if (byte == '\n'.code) {
                val line = currentLine.toString().trim()
                headerLines.add(line)

                // Parse important header lines
                if (line.startsWith("format")) {
                    val parts = line.split(" ")
                    if (parts.size >= 2) {
                        format = parts[1]
                    }
                }

                if (line.startsWith("element vertex")) {
                    val parts = line.split(" ")
                    if (parts.size >= 3) {
                        vertexCount = parts[2].toInt()
                    }
                }

                if (line == "end_header") {
                    break
                }

                currentLine = StringBuilder()
            } else {
                currentLine.append(byte.toChar())
            }
        }

        return Pair(vertexCount, format)
    }

    private fun readLittleEndianFloat(stream: DataInputStream): Float {
        val bytes = ByteArray(4)
        stream.readFully(bytes)

        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return buffer.float
    }

    /**
     * Generate normals for point cloud data using simple radial estimation
     * Optimized for large point clouds - O(n) complexity instead of O(n²)
     */
    private fun generateNormals(vertices: FloatArray, vertexCount: Int, boundingBox: BoundingBox): FloatArray {
        val normals = FloatArray(vertexCount * 3)

        val centerX = boundingBox.centerX
        val centerY = boundingBox.centerY
        val centerZ = boundingBox.centerZ

        android.util.Log.d("PLYLoader", "Generating normals for $vertexCount vertices...")

        // Simple and fast: use point-to-center direction as normal
        // This works well for room scans where surfaces face outward
        for (i in 0 until vertexCount) {
            val px = vertices[i * 3]
            val py = vertices[i * 3 + 1]
            val pz = vertices[i * 3 + 2]

            // Vector from center to point
            val vx = px - centerX
            val vy = py - centerY
            val vz = pz - centerZ

            val vlen = kotlin.math.sqrt(vx * vx + vy * vy + vz * vz)
            if (vlen > 0.0001f) {
                normals[i * 3] = vx / vlen
                normals[i * 3 + 1] = vy / vlen
                normals[i * 3 + 2] = vz / vlen
            } else {
                // Default to up vector
                normals[i * 3] = 0f
                normals[i * 3 + 1] = 1f
                normals[i * 3 + 2] = 0f
            }
        }

        android.util.Log.d("PLYLoader", "Normal generation complete")
        return normals
    }
}
