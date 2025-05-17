package com.moviles.studentcoursessystem.model

data class Course(
    val id: Int? = null,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val schedule: String,
    val professor: String
)