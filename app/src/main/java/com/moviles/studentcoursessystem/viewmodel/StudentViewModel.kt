package com.moviles.studentcoursessystem.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.model.Student
import com.moviles.studentcoursessystem.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import com.moviles.studentcoursessystem.common.NetworkUtils.hasNetwork
import com.moviles.studentcoursessystem.data.database.StudentCourseDatabase
import com.moviles.studentcoursessystem.data.database.entity.StudentEntity
import com.moviles.studentcoursessystem.network.ResponseOriginTracker

/**
 * ViewModel responsible for managing student-related data operations
 * Ensures synchronization between remote API and local Room database
 */
class StudentViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val db = StudentCourseDatabase.getDatabase(context)
    private val studentDao = db.studentDao()
    private val courseDao = db.courseDao()

    private val _dataSource = MutableStateFlow<String?>(null)
    val dataSource: StateFlow<String?> = _dataSource

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _courseName = MutableStateFlow<String?>(null)
    val courseName: StateFlow<String?> = _courseName

    /**
     * Fetch a specific student by ID.
     * Tries remote API first, then local DB as fallback.
     * Updates local DB on successful remote fetch.
     */
    fun fetchStudent(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                var studentEntity: StudentEntity? = null

                if (hasNetwork(context)) {
                    // Fetch from API
                    val result = RetrofitInstance.apiStudent.getStudentById(id)
                    studentEntity = result.id?.let {
                        StudentEntity(
                            id = it,
                            name = result.name,
                            email = result.email,
                            phone = result.phone,
                            courseId = result.courseId
                        )
                    }

                    // Synchronize local DB: replace existing entry with fresh data
                    studentEntity?.let {
                        studentDao.insertStudent(it)
                    }

                    _dataSource.value = ResponseOriginTracker.source
                } else {
                    _dataSource.value = "LOCAL"
                }

                // Load student from local DB (either fresh or fallback)
                val localStudent = studentDao.getStudentById(id)
                _selectedStudent.value = localStudent?.toStudent()

                // Load course name from local DB
                val courseName = localStudent?.courseId?.let { courseDao.getCourseById(it)?.name }
                _courseName.value = courseName ?: ""

                Log.i("StudentViewModel", "Fetched student: ${_selectedStudent.value?.name}")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching student: ${e.message}")
                _dataSource.value = "ERROR"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch all students for a given course.
     * Tries remote API first, then local DB as fallback.
     * Updates local DB on successful remote fetch.
     */
    fun fetchStudentsForCourse(courseId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (hasNetwork(context)) {
                    // Fetch from API and map to entities
                    val apiStudents = RetrofitInstance.apiStudent.getStudentsByCourseId(courseId)
                    val studentEntities = apiStudents.map {
                        StudentEntity(
                            id = it.id!!,
                            name = it.name,
                            email = it.email,
                            phone = it.phone,
                            courseId = courseId
                        )
                    }

                    // Synchronize local DB: replace all students of the course
                    studentDao.deleteStudentsByCourse(courseId)
                    studentDao.insertStudents(studentEntities)

                    _dataSource.value = ResponseOriginTracker.source
                } else {
                    _dataSource.value = "LOCAL"
                }

                // Load students from local DB regardless of network status
                val localStudents = studentDao.getStudentsByCourse(courseId)
                _students.value = localStudents.map { it.toStudent() }

                Log.i("StudentViewModel", "Fetched ${_students.value.size} students for course $courseId")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching students for course $courseId: ${e.message}")
                _students.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a student by ID.
     * Attempts remote API call and updates local DB accordingly.
     * Updates UI state upon success.
     */
    fun deleteStudent(studentId: Int?) {
        if (studentId == null) {
            Log.e("StudentViewModel", "Cannot delete student: ID is null")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("StudentViewModel", "Deleting student with ID: $studentId")

                val response = if (hasNetwork(context)) {
                    RetrofitInstance.apiStudent.deleteStudent(studentId)
                } else {
                    null // No network, skip remote delete
                }

                if (response?.isSuccessful == true || response == null) {
                    // Delete locally regardless of network status to keep UI consistent
                    studentDao.deleteStudent(studentId)
                    _students.value = _students.value.filter { it.id != studentId }
                    Log.i("StudentViewModel", "Student deleted locally and remotely (if possible): $studentId")
                } else {
                    Log.e("StudentViewModel", "Failed to delete student remotely: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error deleting student: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a new student.
     * Tries remote API first; upon success, adds to local DB and UI state.
     */
    fun addStudent(
        name: String,
        email: String,
        phone: String,
        courseId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("StudentViewModel", "Adding student: $name, $email, $phone, course $courseId")

                if (!hasNetwork(context)) {
                    onError("No internet connection")
                    return@launch
                }

                // Create student object for API call (id=null since server assigns it)
                val newStudent = Student(
                    id = null,
                    name = name,
                    email = email,
                    phone = phone,
                    courseId = courseId
                )

                // Call API to add student
                val addedStudent = RetrofitInstance.apiStudent.addStudent(newStudent)

                // Insert into local DB
                studentDao.insertStudent(addedStudent.toEntity())

                // Update UI state list
                _students.value = _students.value + addedStudent

                Log.i("StudentViewModel", "Student added with ID: ${addedStudent.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error adding student: ${e.message}")
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing student.
     * Tries remote API first; upon success, updates local DB and UI state.
     */
    fun updateStudent(
        student: Student,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("StudentViewModel", "Updating student ID: ${student.id}")

                if (!hasNetwork(context)) {
                    onError("No internet connection")
                    return@launch
                }

                val updatedStudent = RetrofitInstance.apiStudent.updateStudent(student.id, student)

                // Update local DB
                studentDao.insertStudent(updatedStudent.toEntity())

                // Update UI state list
                _students.value = _students.value.map {
                    if (it.id == updatedStudent.id) updatedStudent else it
                }

                Log.i("StudentViewModel", "Student updated: ${updatedStudent.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error updating student: ${e.message}")
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDataSource() {
        _dataSource.value = null
    }

    // --- Extension helper functions to convert between Entity and Model ---

    private fun StudentEntity.toStudent() = Student(
        id = this.id,
        name = this.name,
        email = this.email,
        phone = this.phone,
        courseId = this.courseId
    )

    private fun Student.toEntity() = StudentEntity(
        id = this.id!!, // Should be non-null after creation
        name = this.name,
        email = this.email,
        phone = this.phone,
        courseId = this.courseId
    )
}
