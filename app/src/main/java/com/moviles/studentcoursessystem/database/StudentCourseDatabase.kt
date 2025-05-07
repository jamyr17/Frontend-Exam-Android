package com.moviles.studentcoursessystem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moviles.studentcoursessystem.dao.CourseDao
import com.moviles.studentcoursessystem.dao.StudentDao
import com.moviles.studentcoursessystem.models.Course
import com.moviles.studentcoursessystem.models.Student

@Database(entities = [Student::class, Course::class], version = 1, exportSchema = false)
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