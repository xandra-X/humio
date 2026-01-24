package com.example.hrmanagement

import android.app.Application
import com.example.hrmanagement.util.TokenManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
    }
}
