package com.moviles.studentcoursessystem.models

data class Student(
    val id: Int?,
    val name: String,
    val email: String,
    val phone: String,
    val courseId: Int
)
