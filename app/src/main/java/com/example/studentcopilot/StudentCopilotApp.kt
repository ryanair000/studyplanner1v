package com.example.studentcopilot

import android.app.Application
import com.example.studentcopilot.data.local.database.AppDatabase
import com.example.studentcopilot.data.repository.AuthRepository
import com.example.studentcopilot.data.repository.SupabaseSyncRepository
import com.example.studentcopilot.reminders.ReminderScheduler

class StudentCopilotApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set
    lateinit var supabaseSyncRepository: SupabaseSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        authRepository = AuthRepository(this, database)
        reminderScheduler = ReminderScheduler(this, database).also { it.ensureNotificationChannel() }
        supabaseSyncRepository = SupabaseSyncRepository(database, authRepository, reminderScheduler)
    }
}
