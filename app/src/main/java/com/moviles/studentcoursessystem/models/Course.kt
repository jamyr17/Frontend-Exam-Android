package com.moviles.studentcoursessystem.models

data class Course(
    val id: Int?,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val schedule: String,
    val professor: String
)
