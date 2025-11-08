package com.example.a10x_assign.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: AnnotationType,
    val wallType: WallType,
    val x: Float,  // X position on the wall (normalized 0-1)
    val y: Float,  // Y position on the wall (normalized 0-1)
    val width: Float,  // Width of annotation (normalized 0-1)
    val height: Float,  // Height of annotation (normalized 0-1)
    val timestamp: Long = System.currentTimeMillis()
)

enum class AnnotationType {
    SPRAY_AREA,
    SAND_AREA,
    OBSTACLE
}

enum class WallType {
    FLOOR,
    CEILING,
    BACK_WALL,
    FRONT_WALL,
    LEFT_WALL,
    RIGHT_WALL
}
