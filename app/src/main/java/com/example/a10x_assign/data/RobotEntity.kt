package com.example.a10x_assign.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "robots")
data class RobotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val x: Float,  // X position in the room
    val y: Float,  // Y position (height)
    val z: Float,  // Z position in the room
    val rotationY: Float = 0f,  // Rotation around Y axis
    val timestamp: Long = System.currentTimeMillis()
)
