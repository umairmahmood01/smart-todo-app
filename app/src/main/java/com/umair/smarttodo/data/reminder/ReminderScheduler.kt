package com.umair.smarttodo.data.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Requests (and cancels) OS-level reminders for tasks and task lists.
 *
 * An interface rather than a direct `WorkManager` call so repositories stay unit testable
 * without the WorkManager runtime - mirrors `EnrichmentScheduler`.
 *
 * Deliberately WorkManager-backed, not `AlarmManager`-backed: this app never requests the
 * exact-alarm permission. A reminder therefore fires at-or-after [atMillis], not exactly at
 * it - WorkManager gives no stronger guarantee than that, which is an accepted trade-off for
 * not touching `SCHEDULE_EXACT_ALARM`.
 */
interface ReminderScheduler {

    /**
     * Schedules (or re-schedules) a reminder for task [taskId] to fire at [atMillis].
     *
     * Calling this again for the same [taskId] **replaces** any previously scheduled
     * reminder rather than stacking a second one - see [ReminderWorker.taskUniqueWorkName].
     * Must return promptly and must never throw.
     */
    fun scheduleTaskReminder(taskId: Long, atMillis: Long)

    /** Same contract as [scheduleTaskReminder], for a [com.umair.smarttodo.domain.TaskList]. */
    fun scheduleListReminder(listId: Long, atMillis: Long)

    /** Cancels any pending reminder for task [taskId]. A no-op if none is scheduled. */
    fun cancelTaskReminder(taskId: Long)

    /** Cancels any pending reminder for list [listId]. A no-op if none is scheduled. */
    fun cancelListReminder(listId: Long)
}

/**
 * WorkManager-backed [ReminderScheduler].
 *
 * Uses unique work keyed on `"reminder-task-$taskId"` / `"reminder-list-$listId"` with
 * [ExistingWorkPolicy.REPLACE], so calling `setReminder` again for the same id always
 * supersedes the previous request instead of racing two deliveries.
 *
 * `WorkManager.getInstance` is resolved lazily inside each call rather than injected, to keep
 * this class independent of WorkManager's initialisation order at startup - same reasoning as
 * `WorkManagerEnrichmentScheduler`.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    override fun scheduleTaskReminder(taskId: Long, atMillis: Long) {
        enqueue(ReminderWorker.taskUniqueWorkName(taskId), ReminderWorker.taskRequest(taskId, atMillis))
    }

    override fun scheduleListReminder(listId: Long, atMillis: Long) {
        enqueue(ReminderWorker.listUniqueWorkName(listId), ReminderWorker.listRequest(listId, atMillis))
    }

    override fun cancelTaskReminder(taskId: Long) {
        cancel(ReminderWorker.taskUniqueWorkName(taskId))
    }

    override fun cancelListReminder(listId: Long) {
        cancel(ReminderWorker.listUniqueWorkName(listId))
    }

    private fun enqueue(uniqueWorkName: String, request: OneTimeWorkRequest) {
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }.onFailure { error ->
            // Scheduling a reminder is best-effort; the write that triggered it (setReminder)
            // has already succeeded in the database and must stay that way.
            Log.w(TAG, "Could not schedule reminder work " + uniqueWorkName, error)
        }
    }

    private fun cancel(uniqueWorkName: String) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        }.onFailure { error ->
            Log.w(TAG, "Could not cancel reminder work " + uniqueWorkName, error)
        }
    }

    private companion object {
        private const val TAG = "ReminderScheduler"
    }
}
