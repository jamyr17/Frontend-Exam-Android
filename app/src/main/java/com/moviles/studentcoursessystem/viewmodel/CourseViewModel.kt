package com.moviles.studentcoursessystem.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.common.NetworkUtils.hasNetwork
import com.moviles.studentcoursessystem.data.database.StudentCourseDatabase
import com.moviles.studentcoursessystem.data.database.entity.CourseEntity
import com.moviles.studentcoursessystem.model.Course
import com.moviles.studentcoursessystem.network.ResponseOriginTracker
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

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val db = StudentCourseDatabase.getDatabase(context)
    private val courseDao = db.courseDao()

    private val _dataSource = MutableStateFlow<String?>(null)
    val dataSource: StateFlow<String?> = _dataSource

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> get() = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    /**
     * Fetches the list of courses.
     * If online, fetches from API and updates local database (Room).
     * If offline, loads from Room only.
     */
    fun fetchCourses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (hasNetwork(context)) {
                    // Fetch from API and store in Room
                    val result = RetrofitInstance.apiCourse.getCourses().map {
                        CourseEntity(
                            id = it.id!!,
                            name = it.name,
                            description = it.description,
                            imageUrl = it.imageUrl,
                            schedule = it.schedule,
                            professor = it.professor
                        )
                    }

                    _dataSource.value = ResponseOriginTracker.source

                    courseDao.deleteAllCourses()
                    courseDao.insertCourses(result)
                } else {
                    _dataSource.value = "LOCAL"
                }

                // Load from Room
                val localCourses = courseDao.getAllCourses()
                _courses.value = localCourses.map {
                    Course(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        schedule = it.schedule,
                        professor = it.professor
                    )
                }

                Log.i("CourseViewModel", "Fetched ${_courses.value.size} courses")
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error fetching courses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a course from the remote API and then from local Room database.
     */
    fun deleteCourse(courseId: Int?) {
        courseId?.let { id ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val response = if (hasNetwork(context)) {
                        RetrofitInstance.apiCourse.deleteCourse(courseId)
                    } else {
                        null // No network, skip remote delete
                    }

                    if (response?.isSuccessful == true || response == null) {
                        // Remove from Room and UI state

                        courseDao.deleteCourse(courseId)
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

    /**
     * Adds a new course via API and inserts it into local Room database.
     */
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

                val imageFile = withContext(Dispatchers.IO) {
                    createTempFileFromUri(context, imageUri)
                }

                if (imageFile == null) {
                    onError("Error processing image file")
                    _isLoading.value = false
                    return@launch
                }

                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val scheduleBody = schedule.toRequestBody("text/plain".toMediaTypeOrNull())
                val professorBody = professor.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

                // Call API
                val newCourse = RetrofitInstance.apiCourse.addCourse(
                    name = nameBody,
                    description = descriptionBody,
                    date = scheduleBody,
                    professor = professorBody,
                    file = imagePart
                )

                // Insert into Room
                courseDao.insertCourses(
                    listOf(
                        CourseEntity(
                            id = newCourse.id!!,
                            name = newCourse.name,
                            description = newCourse.description,
                            schedule = newCourse.schedule,
                            professor = newCourse.professor,
                            imageUrl = newCourse.imageUrl
                        )
                    )
                )

                // Update UI
                _courses.value = courseDao.getAllCourses().map {
                    Course(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        schedule = it.schedule,
                        professor = it.professor
                    )
                }

                Log.i("CourseViewModel", "Successfully added new course with ID: ${newCourse.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error adding course: ${e.message}")
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates a course via API and also updates local Room database.
     */
    fun updateCourse(
        course: Course,
        imageUri: Uri,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val imageFile = withContext(Dispatchers.IO) {
                    createTempFileFromUri(context, imageUri)
                }

                if (imageFile == null) {
                    onError("Error processing image file")
                    _isLoading.value = false
                    return@launch
                }

                val nameBody = course.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionBody = course.description.toRequestBody("text/plain".toMediaTypeOrNull())
                val scheduleBody = course.schedule.toRequestBody("text/plain".toMediaTypeOrNull())
                val professorBody = course.professor.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

                // Call API
                val updatedCourse = RetrofitInstance.apiCourse.updateCourse(
                    course.id,
                    name = nameBody,
                    description = descriptionBody,
                    date = scheduleBody,
                    professor = professorBody,
                    file = imagePart
                )

                // Update Room
                courseDao.insertCourses(
                    listOf(
                        CourseEntity(
                            id = updatedCourse.id!!,
                            name = updatedCourse.name,
                            description = updatedCourse.description,
                            schedule = updatedCourse.schedule,
                            professor = updatedCourse.professor,
                            imageUrl = updatedCourse.imageUrl
                        )
                    )
                )

                // Update UI
                _courses.value = courseDao.getAllCourses().map {
                    Course(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        schedule = it.schedule,
                        professor = it.professor
                    )
                }

                Log.i("CourseViewModel", "Successfully updated course with ID: ${updatedCourse.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error updating course: ${e.message}")
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Utility function to convert image Uri to a temporary file in cache directory.
     */
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

    /**
     * Resets the data source state, used for UI logic.
     */
    fun clearDataSource() {
        _dataSource.value = null
    }
}
