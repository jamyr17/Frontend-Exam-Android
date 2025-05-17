package com.moviles.studentcoursessystem.model

data class Student(
    val id: Int? = null,
    val name: String,
    val email: String,
    val phone: String,
    val courseId: Int
)