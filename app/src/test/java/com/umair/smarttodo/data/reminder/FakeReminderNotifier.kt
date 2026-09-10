package com.umair.smarttodo.data.reminder

/**
 * Records posted notifications instead of touching `NotificationManagerCompat`.
 *
 * Lets [ReminderPosterTest] verify what [ReminderPoster] decided to post - and, just as
 * importantly, what it decided *not* to post - without any Android framework dependency.
 */
class FakeReminderNotifier : ReminderNotifier {

    /** Every (taskId, title) pair passed to [notifyTask], in order. */
    val taskNotifications: MutableList<Pair<Long, String>> = mutableListOf()

    /** Every (listId, title) pair passed to [notifyList], in order. */
    val listNotifications: MutableList<Pair<Long, String>> = mutableListOf()

    override fun notifyTask(taskId: Long, title: String) {
        taskNotifications += taskId to title
    }

    override fun notifyList(listId: Long, title: String) {
        listNotifications += listId to title
    }
}
