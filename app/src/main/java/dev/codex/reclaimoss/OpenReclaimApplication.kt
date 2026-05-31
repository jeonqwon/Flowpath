package dev.codex.reclaimoss

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OpenReclaimApplication : Application() {
    lateinit var appGraph: AppGraph
        private set
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appGraph = AppGraph(this)
        appGraph.startBackgroundObservers(applicationScope)
    }
}

