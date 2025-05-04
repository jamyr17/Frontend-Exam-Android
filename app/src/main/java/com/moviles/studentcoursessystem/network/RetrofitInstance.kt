package com.moviles.studentcoursessystem.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.moviles.studentcoursessystem.common.Constants.API_BASE_URL

object RetrofitInstance {

    val apiCourse: ApiCourseService by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiCourseService::class.java)
    }

    val apiStudent: ApiStudentService by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiStudentService::class.java)
    }
}