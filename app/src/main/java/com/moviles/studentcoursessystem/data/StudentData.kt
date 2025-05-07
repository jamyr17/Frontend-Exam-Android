package com.moviles.studentcoursessystem.data

import android.util.Log
import com.moviles.studentcoursessystem.dao.StudentDao
import com.moviles.studentcoursessystem.models.Student
import com.moviles.studentcoursessystem.network.ApiStudentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class StudentData(
    private val studentDao: StudentDao,
    private val apiStudentService: ApiStudentService
) {
    // Get students from local database
    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()

    // Get students by course from local database
    fun getStudentsByCourse(courseId: Int): Flow<List<Student>> = studentDao.getStudentsByCourse(courseId)

    // Get student by ID from local database
    suspend fun getStudentById(id: Int?): Student? {
        return studentDao.getStudentById(id)
    }

    // Refresh students from API and update local database
    suspend fun refreshStudents() {
        withContext(Dispatchers.IO) {
            try {
                val students = apiStudentService.getStudents()
                studentDao.clearAllStudents()
                studentDao.insertStudents(students)
                Log.d("StudentData", "Refreshed ${students.size} students from API")
            } catch (e: IOException) {
                Log.e("StudentData", "Error refreshing students", e)
            }
        }
    }

    // Refresh students for a specific course from API
    suspend fun refreshStudentsByCourse(courseId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val students = apiStudentService.getStudentsByCourseId(courseId)
                // Remove existing students for this course and add new ones
                studentDao.deleteStudentsByCourse(courseId)
                studentDao.insertStudents(students)
                Log.d("StudentData", "Refreshed ${students.size} students for course $courseId from API")
            } catch (e: IOException) {
                Log.e("StudentData", "Error refreshing students for course $courseId", e)
            }
        }
    }

    // Add a new student
    suspend fun addStudent(student: Student): Result<Student> {
        return withContext(Dispatchers.IO) {
            try {
                val newStudent = apiStudentService.addStudent(student)
                studentDao.insertStudent(newStudent)
                Result.success(newStudent)
            } catch (e: Exception) {
                Log.e("StudentData", "Error adding student", e)
                Result.failure(e)
            }
        }
    }

    // Update an existing student
    suspend fun updateStudent(student: Student): Result<Student> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedStudent = apiStudentService.updateStudent(student.id, student)
                studentDao.updateStudent(updatedStudent)
                Result.success(updatedStudent)
            } catch (e: Exception) {
                Log.e("StudentData", "Error updating student", e)
                Result.failure(e)
            }
        }
    }

    // Delete a student
    suspend fun deleteStudent(id: Int?): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiStudentService.deleteStudent(id)
                if (response.isSuccessful) {
                    studentDao.deleteStudent(id)
                    Result.success(true)
                } else {
                    Result.failure(IOException("Error deleting student: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("StudentData", "Error deleting student", e)
                Result.failure(e)
            }
        }
    }
}