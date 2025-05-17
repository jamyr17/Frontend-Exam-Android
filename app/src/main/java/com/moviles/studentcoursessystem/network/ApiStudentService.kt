package com.moviles.studentcoursessystem.network

import com.moviles.studentcoursessystem.model.Student
import retrofit2.Response
import retrofit2.http.*

interface ApiStudentService {

    /**
     * Fetches the list of students enrolled in a specific course from the server.
     * Should be cached locally to support offline access to student lists per course.
     */
    @GET("api/student/course/{id}")
    suspend fun getStudentsByCourseId(@Path("id") id: Int?): List<Student>

    /**
     * Retrieves a specific student by their ID from the server.
     * Optionally cache in local Room database for offline lookup.
     */
    @GET("api/student/{id}")
    suspend fun getStudentById(@Path("id") id: Int?): Student

    /**
     * Sends a new student to be created on the server.
     * After successful creation, the student should also be inserted into the local Room database.
     */
    @POST("api/student")
    suspend fun addStudent(@Body student: Student): Student

    /**
     * Updates an existing student on the server.
     * This update should also be applied to the local Room database.
     */
    @PUT("api/student/{id}")
    suspend fun updateStudent(
        @Path("id") id: Int?,
        @Body studentDto: Student
    ): Student

    /**
     * Deletes a student from the server using their ID.
     * The student should also be removed from the local Room database.
     */
    @DELETE("api/student/{id}")
    suspend fun deleteStudent(@Path("id") id: Int?): Response<Unit>
}
