package com.moviles.studentcoursessystem.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.models.Student
import com.moviles.studentcoursessystem.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ViewModel for managing student data operations
 * Handles fetching, adding, updating, and deleting students
 */
class StudentViewModel : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> get() = _students

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    /**
     * Fetch all students from the API
     */
    fun fetchStudents() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _students.value = RetrofitInstance.apiStudent.getStudents()
                Log.i("StudentViewModel", "Fetched ${_students.value.size} students from API")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching students: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch for an specific student
     */
    fun fetchStudent(id: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val student = RetrofitInstance.apiStudent.getStudentById(id)
                Log.i("StudentViewModel", "Fetched student: ${student.name} from API")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching students: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch students for a specific course
     * @param courseId ID of the course to fetch students for
     */
    fun fetchStudentsForCourse(courseId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _students.value = RetrofitInstance.apiStudent.getStudentsByCourseId(courseId)
                Log.i("StudentViewModel", "Fetched ${_students.value.size} students for course $courseId")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching students for course $courseId: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a student from the system
     * @param studentId ID of the student to delete
     */
    fun deleteStudent(studentId: Int?) {
        studentId?.let { id ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val response = RetrofitInstance.apiStudent.deleteStudent(id)

                    if (response.isSuccessful) {
                        // Update local state to remove the deleted student
                        _students.value = _students.value.filter { it.id != studentId }
                        Log.i("StudentViewModel", "Successfully deleted student with ID: $id")
                    } else {
                        Log.e("StudentViewModel", "Error deleting student: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("StudentViewModel", "Error deleting student: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: Log.e("StudentViewModel", "Cannot delete student: ID is null")
    }

    /**
     * Add a new student to the system
     * @param name Student's name
     * @param email Student's email
     * @param phone Student's phone number
     * @param courseId ID of the course to associate with the student
     * @param onSuccess Callback for successful addition
     * @param onError Callback for error handling
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

                // Convert parameters to RequestBody objects
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
                val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
                val courseIdBody = courseId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // Make API call
                val newStudent = RetrofitInstance.apiStudent.addStudent(
                    name = nameBody,
                    email = emailBody,
                    phone = phoneBody,
                    courseId = courseIdBody
                )

                // Update the local state
                _students.value = _students.value + newStudent
                Log.i("StudentViewModel", "Successfully added new student with ID: ${newStudent.id}")

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
     * Update an existing student
     * @param student Updated student object
     * @param onSuccess Callback for successful update
     * @param onError Callback for error handling
     */
    fun updateStudent(
        student: Student,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedStudent = RetrofitInstance.apiStudent.updateStudent(student.id, student)

                // Update the local state
                _students.value = _students.value.map {
                    if (it.id == updatedStudent.id) updatedStudent else it
                }

                Log.i("StudentViewModel", "Successfully updated student with ID: ${updatedStudent.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error updating student: ${e.message}")
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }
}