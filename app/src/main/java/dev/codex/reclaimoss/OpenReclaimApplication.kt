package dev.codex.reclaimoss

import android.app.Application

class OpenReclaimApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = AppGraph(this)
    }
}

