package com.moviles.studentcoursessystem.network

import com.moviles.studentcoursessystem.model.Course
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiCourseService {

    /**
     * Fetches all courses from the remote server.
     * Note: This should be followed by caching the result into the local Room database.
     */
    @GET("api/course")
    suspend fun getCourses(): List<Course>

    /**
     * Sends a multipart request to create a new course on the server.
     * Includes course metadata and an image file.
     * After successful creation, the course should also be saved in the local Room database.
     */
    @Multipart
    @POST("api/course")
    suspend fun addCourse(
        @Part("Name") name: RequestBody,
        @Part("Description") description: RequestBody,
        @Part("Schedule") date: RequestBody,
        @Part("Professor") professor: RequestBody,
        @Part file: MultipartBody.Part
    ): Course

    /**
     * Updates an existing course on the server with new data and image.
     * This update should also be reflected in the local Room database.
     */
    @Multipart
    @PUT("api/course/{id}")
    suspend fun updateCourse(
        @Path("id") id: Int?,
        @Part("Name") name: RequestBody,
        @Part("Description") description: RequestBody,
        @Part("Schedule") date: RequestBody,
        @Part("Professor") professor: RequestBody,
        @Part file: MultipartBody.Part
    ): Course

    /**
     * Deletes a course from the server using its ID.
     * After successful deletion, the course should also be removed from the local Room database.
     */
    @DELETE("api/course/{id}")
    suspend fun deleteCourse(@Path("id") id: Int?): Response<Unit>
}
