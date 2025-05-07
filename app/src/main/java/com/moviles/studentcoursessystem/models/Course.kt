package com.moviles.studentcoursessystem.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val schedule: String,
    val professor: String,
    val isFromCache: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)