package com.moviles.studentcoursessystem.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.models.Student
import com.moviles.studentcoursessystem.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing student data operations
 * Handles fetching, adding, updating, and deleting students
 */
class StudentViewModel : ViewModel() {

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _courseName = MutableStateFlow<String?>(null)
    val courseName: StateFlow<String?> = _courseName

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
                _selectedStudent.value = student

                val course = RetrofitInstance.apiCourse.getCourseById(student.courseId)
                _courseName.value = course.name

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
                Log.d("StudentViewModel", "Fetching students for course ID: $courseId")
                val result = RetrofitInstance.apiStudent.getStudentsByCourseId(courseId)
                _students.value = result
                Log.i("StudentViewModel", "Fetched ${_students.value.size} students for course $courseId")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error fetching students for course $courseId: ${e.message}")
                _students.value = emptyList() // Reset to empty list on error
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
                    Log.d("StudentViewModel", "Attempting to delete student with ID: $id")
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
                // Debugging info
                Log.d("StudentViewModel", "Adding student: $name, email: $email, phone: $phone, courseId: $courseId")

                // Create a Student object instead of using RequestBody
                val student = Student(
                    id = null, // ID will be assigned by the server
                    name = name,
                    email = email,
                    phone = phone,
                    courseId = courseId
                )

                // Make API call with the Student object
                val newStudent = RetrofitInstance.apiStudent.addStudent(student)

                // Update the local state
                _students.value = _students.value + newStudent
                Log.i("StudentViewModel", "Successfully added new student with ID: ${newStudent.id}, courseId: ${newStudent.courseId}")

                onSuccess()
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error adding student: ${e.message}", e)
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
                Log.d("StudentViewModel", "Updating student with ID: ${student.id}, courseId: ${student.courseId}")

                val updatedStudent = RetrofitInstance.apiStudent.updateStudent(student.id, student)

                // Update the local state
                _students.value = _students.value.map {
                    if (it.id == updatedStudent.id) updatedStudent else it
                }

                Log.i("StudentViewModel", "Successfully updated student with ID: ${updatedStudent.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Error updating student: ${e.message}", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }
}