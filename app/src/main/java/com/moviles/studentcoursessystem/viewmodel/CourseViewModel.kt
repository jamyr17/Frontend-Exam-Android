package com.moviles.studentcoursessystem.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviles.studentcoursessystem.models.Course
import com.moviles.studentcoursessystem.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CourseViewModel : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> get() = _courses

    fun fetchCourses() {
        viewModelScope.launch {
            try {
                _courses.value = RetrofitInstance.apiCourse.getCourses()
                Log.i("CourseViewModel", "Fetching data from API Course... ${_courses.value}")
            } catch (e: Exception) {
                Log.e("CourseViewModelError", "Error: ${e}")
            }
        }
    }

    fun deleteEvent(courseId: Int?) {
        courseId?.let { id ->
            viewModelScope.launch {
                try {
                    RetrofitInstance.apiCourse.deleteCourse(id)
                    _courses.value = _courses.value.filter { it.id != courseId }
                } catch (e: Exception) {
                    Log.e("CourseViewModelError", "Error deleting course: ${e.message}")
                }
            }
        } ?: Log.e("CourseViewModelError", "Error: courseId is null")
    }

}