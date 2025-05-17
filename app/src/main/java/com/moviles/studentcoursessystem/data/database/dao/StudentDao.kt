package com.moviles.studentcoursessystem.data.database.dao

import androidx.room.*
import com.moviles.studentcoursessystem.data.database.entity.StudentEntity

@Dao
interface StudentDao {

    // Retrieves all students belonging to a specific course, ordered alphabetically by name
    @Query("SELECT * FROM students WHERE courseId = :courseId ORDER BY name ASC")
    suspend fun getStudentsByCourse(courseId: Int): List<StudentEntity>

    // Retrieves a single student by their unique ID
    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int?): StudentEntity?

    // Inserts or updates a list of students in the database
    // Consider wrapping this with a transaction if combined with deletions or updates for atomicity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    // Inserts or updates a single student, returning the row ID of the inserted student
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    // Deletes a student by their ID
    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Int?)

    // Deletes all students associated with a specific course
    // Use with caution: ensure this operation aligns with your data consistency requirements
    @Query("DELETE FROM students WHERE courseId = :courseId")
    suspend fun deleteStudentsByCourse(courseId: Int)
}
