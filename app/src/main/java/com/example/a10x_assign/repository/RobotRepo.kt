package com.example.a10x_assign.repository

import com.example.a10x_assign.data.RobotDao
import com.example.a10x_assign.data.RobotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RobotRepo @Inject constructor(
    private val robotDao: RobotDao
) {
    fun getRobotPosition(): Flow<RobotEntity?> {
        return robotDao.getRobotPosition()
    }

    suspend fun insertRobot(robot: RobotEntity): Long {
        return robotDao.insert(robot)
    }

    suspend fun deleteRobot(robot: RobotEntity) {
        robotDao.delete(robot)
    }

    suspend fun clearRobot() {
        robotDao.deleteAll()
    }
}
