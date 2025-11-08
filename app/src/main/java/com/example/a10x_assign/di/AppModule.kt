package com.example.a10x_assign.di

import android.content.Context
import com.example.a10x_assign.data.AnnotationDao
import com.example.a10x_assign.data.AppDatabase
import com.example.a10x_assign.data.RobotDao
import com.example.a10x_assign.opengl.AnnotationOverlay
import com.example.a10x_assign.opengl.Camera
import com.example.a10x_assign.opengl.RayCaster
import com.example.a10x_assign.opengl.RobotCube
import com.example.a10x_assign.opengl.Room
import com.example.a10x_assign.opengl.RoomRenderer
import com.example.a10x_assign.opengl.TextRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCamera(): Camera {
        return Camera()
    }

    @Provides
    @Singleton
    fun provideRoom(): Room {
        return Room()
    }

    @Provides
    @Singleton
    fun provideRobotCube(): RobotCube {
        return RobotCube()
    }

    @Provides
    @Singleton
    fun provideAnnotationOverlay(): AnnotationOverlay {
        return AnnotationOverlay()
    }

    @Provides
    @Singleton
    fun provideRayCaster(): RayCaster {
        return RayCaster()
    }

    @Provides
    @Singleton
    fun provideTextRenderer(): TextRenderer {
        return TextRenderer()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAnnotationDao(database: AppDatabase): AnnotationDao {
        return database.annotationDao()
    }

    @Provides
    @Singleton
    fun provideRobotDao(database: AppDatabase): RobotDao {
        return database.robotDao()
    }
}

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityScoped
    fun provideRoomRenderer(
        camera: Camera,
        room: Room,
        robotCube: RobotCube,
        annotationOverlay: AnnotationOverlay,
        textRenderer: TextRenderer
    ): RoomRenderer {
        return RoomRenderer(camera, room, robotCube, annotationOverlay, textRenderer)
    }
}
