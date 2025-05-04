package com.moviles.studentcoursessystem.network

import com.moviles.studentcoursessystem.models.Course
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiCourseService {
    @GET("api/course")
    suspend fun getCourses(): List<Course>

    @Multipart
    @POST("api/course")
    suspend fun addCourse(
        @Part("Name") name: RequestBody,
        @Part("Description") description: RequestBody,
        @Part("Schedule") date: RequestBody,
        @Part("Professor") professor: RequestBody,
        @Part file: MultipartBody.Part
    ): Course

    @PUT("api/course/{id}")
    suspend fun updateCourse(@Path("id") id: Int?, @Body courseDto: Course): Course

    @DELETE("api/course/{id}")
    suspend fun deleteCourse(@Path("id") id: Int?): Response<Unit>
}