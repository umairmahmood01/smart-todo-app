package com.umair.smarttodo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.umair.smarttodo.domain.ReminderChannel
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

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel()
    }

    /**
     * Creates the single notification channel every task/list reminder posts on, once, at
     * process start.
     *
     * Creating a channel needs no runtime permission - only *posting* to it later requires
     * `POST_NOTIFICATIONS` at runtime (API 33+), which is a UI-layer concern elsewhere.
     * `createNotificationChannel` is idempotent: calling it again on every process start with
     * the same id/settings does not duplicate or reset user customisations of the channel.
     */
    private fun createReminderNotificationChannel() {
        val channel = NotificationChannel(
            ReminderChannel.ID,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.reminder_channel_description)
        }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }
}
