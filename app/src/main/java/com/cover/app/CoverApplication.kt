package com.cover.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CoverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
