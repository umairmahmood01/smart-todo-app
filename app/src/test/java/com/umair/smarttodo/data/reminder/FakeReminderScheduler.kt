package com.umair.smarttodo.data.reminder

/**
 * Records scheduling/cancellation requests instead of touching WorkManager.
 *
 * Keeps repository tests pure JVM tests while still proving that `setReminder` calls the
 * scheduler with the right arguments - mirrors `FakeEnrichmentScheduler`.
 */
class FakeReminderScheduler : ReminderScheduler {

    /** Every (taskId, atMillis) pair passed to [scheduleTaskReminder], in order. */
    val scheduledTasks: MutableList<Pair<Long, Long>> = mutableListOf()

    /** Every taskId passed to [cancelTaskReminder], in order. */
    val cancelledTasks: MutableList<Long> = mutableListOf()

    /** Every (listId, atMillis) pair passed to [scheduleListReminder], in order. */
    val scheduledLists: MutableList<Pair<Long, Long>> = mutableListOf()

    /** Every listId passed to [cancelListReminder], in order. */
    val cancelledLists: MutableList<Long> = mutableListOf()

    override fun scheduleTaskReminder(taskId: Long, atMillis: Long) {
        scheduledTasks += taskId to atMillis
    }

    override fun scheduleListReminder(listId: Long, atMillis: Long) {
        scheduledLists += listId to atMillis
    }

    override fun cancelTaskReminder(taskId: Long) {
        cancelledTasks += taskId
    }

    override fun cancelListReminder(listId: Long) {
        cancelledLists += listId
    }
}
