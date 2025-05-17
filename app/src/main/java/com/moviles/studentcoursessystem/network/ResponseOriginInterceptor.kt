package com.moviles.studentcoursessystem.network

import okhttp3.Interceptor
import okhttp3.Response

class ResponseOriginInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        ResponseOriginTracker.source = when {
            response.networkResponse != null -> "INTERNET"
            response.cacheResponse != null -> "CACHE"
            else -> "UNKNOWN"
        }

        return response
    }
}
