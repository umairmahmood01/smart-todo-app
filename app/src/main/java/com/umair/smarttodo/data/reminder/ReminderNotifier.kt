package com.umair.smarttodo.data.reminder

/**
 * Posts the actual OS notification for a fired reminder.
 *
 * Split out from [ReminderPoster] purely for testability: [ReminderPoster]'s "should this
 * reminder post at all" decision is plain suspend-function logic that a JVM unit test can
 * drive with a fake; only this interface's real implementation
 * ([SystemReminderNotifier]) touches `NotificationManagerCompat` / `PendingIntent`.
 */
interface ReminderNotifier {

    /** Posts (or silently skips, if not permitted) a notification for task [taskId]. */
    fun notifyTask(taskId: Long, title: String)

    /** Posts (or silently skips, if not permitted) a notification for list [listId]. */
    fun notifyList(listId: Long, title: String)
}
