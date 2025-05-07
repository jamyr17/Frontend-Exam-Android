package com.moviles.studentcoursessystem.dao

import androidx.room.*
import com.moviles.studentcoursessystem.models.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE courseId = :courseId ORDER BY name ASC")
    fun getStudentsByCourse(courseId: Int): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int?): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Int?)

    @Query("DELETE FROM students")
    suspend fun clearAllStudents()

    @Query("DELETE FROM students WHERE courseId = :courseId")
    suspend fun deleteStudentsByCourse(courseId: Int)
}