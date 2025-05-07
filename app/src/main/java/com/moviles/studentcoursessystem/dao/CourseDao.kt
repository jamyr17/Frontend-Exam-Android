package com.moviles.studentcoursessystem.dao

import androidx.room.*
import com.moviles.studentcoursessystem.models.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int?): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: Int?)

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()

    @Query("SELECT COUNT(*) FROM students WHERE courseId = :courseId")
    suspend fun getStudentCountForCourse(courseId: Int): Int
}