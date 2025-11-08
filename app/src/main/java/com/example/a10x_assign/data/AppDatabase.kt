package com.example.a10x_assign.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [AnnotationEntity::class, RobotEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun annotationDao(): AnnotationDao
    abstract fun robotDao(): RobotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "robot_operator_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromAnnotationType(value: AnnotationType): String {
        return value.name
    }

    @TypeConverter
    fun toAnnotationType(value: String): AnnotationType {
        return AnnotationType.valueOf(value)
    }

    @TypeConverter
    fun fromWallType(value: WallType): String {
        return value.name
    }

    @TypeConverter
    fun toWallType(value: String): WallType {
        return WallType.valueOf(value)
    }
}
