package com.example.myagent

import android.app.Application
import android.content.Context
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class MyAgentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@MyAgentApp, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = "MyAgent/1.0 (Android; ${BuildConfig.APPLICATION_ID})"
            Log.d("OSM", "UserAgent = $userAgentValue")
        }
    }
}