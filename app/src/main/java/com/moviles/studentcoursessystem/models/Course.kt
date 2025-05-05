package com.moviles.studentcoursessystem.models

data class Course(
    val id: Int? = null,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val schedule: String,
    val professor: String
)
