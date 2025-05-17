package com.moviles.studentcoursessystem

import android.app.Application
import android.content.Context
import com.moviles.studentcoursessystem.network.RetrofitInstance

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        RetrofitInstance.init(this)
    }

    companion object {
        private lateinit var instance: MyApplication
        val context: Context
            get() = instance.applicationContext
    }
}
