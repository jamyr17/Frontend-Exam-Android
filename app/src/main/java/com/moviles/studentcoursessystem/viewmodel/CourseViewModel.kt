package com.moviles.studentcoursessystem.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.models.Course
import com.moviles.studentcoursessystem.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CourseViewModel : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> get() = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // Fetch all courses
    fun fetchCourses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _courses.value = RetrofitInstance.apiCourse.getCourses()
                Log.i("CourseViewModel", "Fetched ${_courses.value.size} courses from API")
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error fetching courses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete a course
    fun deleteEvent(courseId: Int?) {
        courseId?.let { id ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val response = RetrofitInstance.apiCourse.deleteCourse(id)

                    if (response.isSuccessful) {
                        // Update local state to remove the deleted course
                        _courses.value = _courses.value.filter { it.id != courseId }
                        Log.i("CourseViewModel", "Successfully deleted course with ID: $id")
                    } else {
                        Log.e("CourseViewModel", "Error deleting course: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error deleting course: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: Log.e("CourseViewModel", "Cannot delete course: ID is null")
    }

    // Add a new course
    fun addCourse(
        name: String,
        description: String,
        schedule: String,
        professor: String,
        imageUri: Uri,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Process the image file (in IO dispatcher)
                val imageFile = withContext(Dispatchers.IO) {
                    createTempFileFromUri(context, imageUri)
                }

                if (imageFile == null) {
                    onError("Error processing image file")
                    _isLoading.value = false
                    return@launch
                }

                // Convert parameters to RequestBody objects
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val scheduleBody = schedule.toRequestBody("text/plain".toMediaTypeOrNull())
                val professorBody = professor.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

                // Make API call
                val newCourse = RetrofitInstance.apiCourse.addCourse(
                    name = nameBody,
                    description = descriptionBody,
                    date = scheduleBody,
                    professor = professorBody,
                    file = imagePart
                )

                // Update the local state
                _courses.value = _courses.value + newCourse
                Log.i("CourseViewModel", "Successfully added new course with ID: ${newCourse.id}")

                onSuccess()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error adding course: ${e.message}")
                onError(e.message ?: "Error desconocido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update an existing course
    fun updateCourse(
        course: Course,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedCourse = RetrofitInstance.apiCourse.updateCourse(course.id, course)

                // Update the local state
                _courses.value = _courses.value.map {
                    if (it.id == updatedCourse.id) updatedCourse else it
                }

                Log.i("CourseViewModel", "Successfully updated course with ID: ${updatedCourse.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error updating course: ${e.message}")
                onError(e.message ?: "Error desconocido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper function to create a temporary file from Uri
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                Log.e("CourseViewModel", "Error: InputStream is null for Uri: $uri")
                return null
            }

            val file = File.createTempFile("img_", ".jpg", context.cacheDir)
            Log.d("CourseViewModel", "Creating temp file at: ${file.absolutePath}")

            stream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4k buffer
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            Log.d("CourseViewModel", "File created successfully, size: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error creating temp file: ${e.message}", e)
            null
        }
    }
}