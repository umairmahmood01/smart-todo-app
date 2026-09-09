package com.umair.smarttodo

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Also supplies WorkManager's configuration so that background workers can be constructed by
 * Hilt. Without the [HiltWorkerFactory] installed here, WorkManager would fall back to
 * reflective instantiation and could not build the `@HiltWorker` enrichment worker, whose
 * constructor takes injected dependencies.
 *
 * This pairs with the removal of WorkManager's App Startup initializer in
 * `AndroidManifest.xml`: the default initializer would otherwise configure WorkManager with a
 * stock factory before this configuration is ever read.
 */
@HiltAndroidApp
class SmartTodoApplication : Application(), Configuration.Provider {

    /** Bridges WorkManager's worker instantiation to the Hilt object graph. */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()
}
