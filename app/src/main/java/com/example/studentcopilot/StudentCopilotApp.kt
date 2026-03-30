package com.example.studentcopilot

import android.app.Application
import com.example.studentcopilot.data.local.database.AppDatabase

class StudentCopilotApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
