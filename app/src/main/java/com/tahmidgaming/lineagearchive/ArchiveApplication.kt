package com.tahmidgaming.lineagearchive

import android.app.Application

class ArchiveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LineageRepository.initialize(this)
    }
}
