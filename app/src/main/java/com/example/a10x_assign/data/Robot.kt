package com.example.a10x_assign.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RobotDao {
    @Query("SELECT * FROM robots ORDER BY timestamp DESC LIMIT 1")
    fun getRobotPosition(): Flow<RobotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(robot: RobotEntity): Long

    @Delete
    suspend fun delete(robot: RobotEntity)

    @Query("DELETE FROM robots")
    suspend fun deleteAll()
}
