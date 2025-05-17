package com.moviles.studentcoursessystem.network

import android.content.Context
import com.moviles.studentcoursessystem.common.Constants.API_BASE_URL
import com.moviles.studentcoursessystem.common.NetworkUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object RetrofitInstance {

    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private lateinit var cache: Cache
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        val cacheDir = File(appContext.cacheDir, "http_cache")
        cache = Cache(cacheDir, CACHE_SIZE.toLong())
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(ResponseOriginInterceptor())
            .addInterceptor { chain ->
                val request = if (NetworkUtils.isNetworkAvailable(appContext)) {
                    chain.request().newBuilder()
                        .header("Cache-Control", "public, max-age=60")
                        .build()
                } else {
                    chain.request().newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                        .build()
                }
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=60")
                    .build()
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiCourse: ApiCourseService by lazy {
        retrofit.create(ApiCourseService::class.java)
    }

    val apiStudent: ApiStudentService by lazy {
        retrofit.create(ApiStudentService::class.java)
    }
}

