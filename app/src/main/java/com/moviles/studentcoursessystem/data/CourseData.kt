package com.moviles.studentcoursessystem.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.moviles.studentcoursessystem.dao.CourseDao
import com.moviles.studentcoursessystem.models.Course
import com.moviles.studentcoursessystem.network.ApiCourseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CourseData(
    private val courseDao: CourseDao,
    private val apiCourseService: ApiCourseService,
    private val context: Context
) {
    // Get courses from local database
    fun getAllCourses(): Flow<List<Course>> = courseDao.getAllCourses()

    // Get course by ID from local database
    suspend fun getCourseById(id: Int?): Course? {
        return courseDao.getCourseById(id)
    }

    // Check if course has students
    suspend fun courseHasStudents(courseId: Int): Boolean {
        return courseDao.getStudentCountForCourse(courseId) > 0
    }

    // Refresh courses from API and update local database
    suspend fun refreshCourses() {
        withContext(Dispatchers.IO) {
            try {
                val courses = apiCourseService.getCourses()
                courseDao.clearAllCourses()
                courseDao.insertCourses(courses)
                Log.d("CourseData", "Refreshed ${courses.size} courses from API")
            } catch (e: IOException) {
                Log.e("CourseData", "Error refreshing courses", e)
            }
        }
    }

    // Add a new course with image upload
    suspend fun addCourse(course: Course, imageUri: Uri?): Result<Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUri == null) {
                    return@withContext Result.failure(IOException("Image URI is null"))
                }

                // Convert image URI to MultipartBody.Part
                val file = uriToFile(imageUri)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                // Create request bodies for course fields
                val namePart = course.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionPart = course.description.toRequestBody("text/plain".toMediaTypeOrNull())
                val schedulePart = course.schedule.toRequestBody("text/plain".toMediaTypeOrNull())
                val professorPart = course.professor.toRequestBody("text/plain".toMediaTypeOrNull())

                // Make API call
                val newCourse = apiCourseService.addCourse(
                    namePart,
                    descriptionPart,
                    schedulePart,
                    professorPart,
                    imagePart
                )

                // Save to local database
                courseDao.insertCourse(newCourse)
                Result.success(newCourse)
            } catch (e: Exception) {
                Log.e("CourseData", "Error adding course", e)
                Result.failure(e)
            }
        }
    }

    // Update an existing course
    suspend fun updateCourse(course: Course): Result<Course> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedCourse = apiCourseService.updateCourse(course.id, course)
                courseDao.updateCourse(updatedCourse)
                Result.success(updatedCourse)
            } catch (e: Exception) {
                Log.e("CourseData", "Error updating course", e)
                Result.failure(e)
            }
        }
    }

    // Delete a course
    suspend fun deleteCourse(id: Int?): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCourseService.deleteCourse(id)
                if (response.isSuccessful) {
                    courseDao.deleteCourse(id)
                    Result.success(true)
                } else {
                    Result.failure(IOException("Error deleting course: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("CourseData", "Error deleting course", e)
                Result.failure(e)
            }
        }
    }

    // Helper method to convert Uri to File
    private fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}