package com.moviles.studentcoursessystem.network

import com.moviles.studentcoursessystem.models.Student
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiStudentService {
    @GET("api/student")
    suspend fun getStudents(): List<Student>

    @GET("api/student/course/{id}")
    suspend fun getStudentsByCourseId(@Path("id") id: Int?): List<Student>

    @Multipart
    @POST("api/student")
    suspend fun addStudent(
        @Part("Name") name: RequestBody,
        @Part("Email") email: RequestBody,
        @Part("Phone") phone: RequestBody,
        @Part("CourseId") courseId: RequestBody
    ): Student

    @PUT("api/student/{id}")
    suspend fun updateStudent(@Path("id") id: Int?, @Body studentDto: Student): Student

    @DELETE("api/student/{id}")
    suspend fun deleteStudent(@Path("id") id: Int?): Response<Unit>
}