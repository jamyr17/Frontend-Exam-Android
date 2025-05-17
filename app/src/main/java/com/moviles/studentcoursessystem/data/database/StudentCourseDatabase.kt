package com.moviles.studentcoursessystem.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moviles.studentcoursessystem.data.database.dao.CourseDao
import com.moviles.studentcoursessystem.data.database.dao.StudentDao
import com.moviles.studentcoursessystem.data.database.entity.CourseEntity
import com.moviles.studentcoursessystem.data.database.entity.StudentEntity

@Database(entities = [StudentEntity::class, CourseEntity::class], version = 1, exportSchema = false)
abstract class StudentCourseDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: StudentCourseDatabase? = null

        fun getDatabase(context: Context): StudentCourseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudentCourseDatabase::class.java,
                    "student_course_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}