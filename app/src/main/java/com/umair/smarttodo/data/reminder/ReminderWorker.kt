package com.umair.smarttodo.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Fires once, at (or shortly after) a scheduled reminder's target time, for either a task or
 * a task list - discriminated by [KEY_TYPE].
 *
 * Deliberately thin: all "should this actually post, and with what title" logic lives in
 * [ReminderPoster], which is plain JVM-testable. This class only unpacks WorkManager's input
 * data and delegates.
 *
 * No retry policy: unlike [com.umair.smarttodo.data.enrichment.TaskEnrichmentWorker], a
 * missed reminder delivery has no well-defined "retry" - by the time WorkManager would retry,
 * the reminder is stale. [doWork] always returns [Result.success] (or [Result.failure] only
 * for malformed input, which cannot happen from this module's own scheduling code).
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val poster: ReminderPoster,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, NO_ID)
        if (id == NO_ID) return Result.failure()

        when (inputData.getString(KEY_TYPE)) {
            TYPE_TASK -> poster.postTaskReminder(id)
            TYPE_LIST -> poster.postListReminder(id)
            else -> return Result.failure()
        }
        return Result.success()
    }

    companion object {
        /** Input key: `"task"` or `"list"`. */
        const val KEY_TYPE: String = "type"

        /** Input key for the task or list row id. */
        const val KEY_ID: String = "id"

        /** [KEY_TYPE] value for a plain-`Task` reminder. */
        const val TYPE_TASK: String = "task"

        /** [KEY_TYPE] value for a `TaskList` reminder. */
        const val TYPE_LIST: String = "list"

        /** Tag for observing or cancelling every reminder job at once. */
        const val TAG: String = "task-reminder"

        private const val NO_ID = -1L
        private const val WORK_NAME_TASK_PREFIX = "reminder-task-"
        private const val WORK_NAME_LIST_PREFIX = "reminder-list-"

        /** Unique work name for task [taskId]; the deduplication/replace key for scheduling. */
        fun taskUniqueWorkName(taskId: Long): String = WORK_NAME_TASK_PREFIX + taskId

        /** Unique work name for list [listId]; the deduplication/replace key for scheduling. */
        fun listUniqueWorkName(listId: Long): String = WORK_NAME_LIST_PREFIX + listId

        /**
         * Delay from "now" until [atMillis], clamped to zero.
         *
         * A reminder set for a moment already in the past (or exactly "now") is not an
         * error - it fires as soon as WorkManager can run it, rather than being rejected or
         * treated as immediately overdue-and-dropped.
         */
        fun computeInitialDelayMillis(atMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long =
            (atMillis - nowMillis).coerceAtLeast(0L)

        /** Builds the request for a task reminder, to be enqueued as [taskUniqueWorkName]. */
        fun taskRequest(taskId: Long, atMillis: Long): OneTimeWorkRequest = request(TYPE_TASK, taskId, atMillis)

        /** Builds the request for a list reminder, to be enqueued as [listUniqueWorkName]. */
        fun listRequest(listId: Long, atMillis: Long): OneTimeWorkRequest = request(TYPE_LIST, listId, atMillis)

        private fun request(type: String, id: Long, atMillis: Long): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(workDataOf(KEY_TYPE to type, KEY_ID to id))
                .setInitialDelay(computeInitialDelayMillis(atMillis), TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()
    }
}
