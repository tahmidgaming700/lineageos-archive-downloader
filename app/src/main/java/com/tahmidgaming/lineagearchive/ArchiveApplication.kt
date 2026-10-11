package com.tahmidgaming.lineagearchive

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ArchiveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LineageRepository.initialize(this)
        AddonRepository.initialize(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val updateCheck = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "lineage-app-update-check",
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheck
        )
    }
}
