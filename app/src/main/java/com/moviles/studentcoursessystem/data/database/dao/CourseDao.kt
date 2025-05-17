package com.moviles.studentcoursessystem.data.database.dao

import androidx.room.*
import com.moviles.studentcoursessystem.data.database.entity.CourseEntity

@Dao
interface CourseDao {

    /**
     * Retrieves all courses stored in the local database, ordered by name.
     * Used for offline access and displaying the course list locally.
     */
    @Query("SELECT * FROM courses ORDER BY name ASC")
    suspend fun getAllCourses(): List<CourseEntity>

    /**
     * Retrieves a specific course by its ID from the local database.
     */
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int?): CourseEntity?

    /**
     * Inserts a list of courses into the local database.
     * If a course already exists, it will be replaced.
     * This is typically used after syncing data from the server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    /**
     * Deletes all courses from the local database.
     * Useful when clearing cache or refreshing data completely from the server.
     */
    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
