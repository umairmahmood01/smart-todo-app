package com.umair.smarttodo.data.reminder

import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.local.TaskListDao
import com.umair.smarttodo.domain.TaskStatus
import javax.inject.Inject

/**
 * Decides, at fire time, whether a reminder should actually post - and if so, with what
 * title - then delegates the OS-level work to [notifier].
 *
 * Deliberately thin and plain-suspend-function, mirroring how `TaskEnricher` is kept separate
 * from `TaskEnrichmentWorker`: this class is what a JVM unit test exercises with a fake DAO
 * and a fake [ReminderNotifier]; [ReminderWorker] itself only unpacks WorkManager input data
 * and calls into here.
 *
 * Always re-reads the current row rather than trusting anything captured when the reminder
 * was scheduled - the text (or the task's status) may have changed since.
 */
class ReminderPoster @Inject constructor(
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
    private val notifier: ReminderNotifier,
) {

    /**
     * Posts the reminder for task [taskId], unless the task has since been deleted or is
     * already [TaskStatus.DONE].
     */
    suspend fun postTaskReminder(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status == TaskStatus.DONE) return
        notifier.notifyTask(taskId, task.normalizedEnglishText ?: task.rawText)
    }

    /**
     * Posts the reminder for list [listId], unless the list has since been deleted.
     *
     * Unlike a task, a [com.umair.smarttodo.domain.TaskList] has no status of its own, so
     * there is no "already done" equivalent to skip on - it posts regardless of how many of
     * its items are checked off.
     */
    suspend fun postListReminder(listId: Long) {
        val list = taskListDao.getTaskListById(listId) ?: return
        notifier.notifyList(listId, list.list.title)
    }
}
