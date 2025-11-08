package com.example.a10x_assign.repository

import com.example.a10x_assign.data.AnnotationDao
import com.example.a10x_assign.data.AnnotationEntity
import com.example.a10x_assign.data.WallType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationRepo @Inject constructor(
    private val annotationDao: AnnotationDao
) {
    fun getAllAnnotations(): Flow<List<AnnotationEntity>> {
        return annotationDao.getAllAnnotations()
    }

    fun getAnnotationsByWall(wallType: WallType): Flow<List<AnnotationEntity>> {
        return annotationDao.getAnnotationsByWall(wallType)
    }

    suspend fun insertAnnotation(annotation: AnnotationEntity): Long {
        return annotationDao.insert(annotation)
    }

    suspend fun deleteAnnotation(annotation: AnnotationEntity) {
        annotationDao.delete(annotation)
    }

    suspend fun clearAllAnnotations() {
        annotationDao.deleteAll()
    }
}
