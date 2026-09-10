package com.umair.smarttodo.data.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.umair.smarttodo.MainActivity
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.ReminderChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ReminderNotifier] backed by `NotificationManagerCompat`.
 *
 * Posts on [ReminderChannel.ID], which is created once at process start in
 * `SmartTodoApplication`. Never crashes on a missing/revoked `POST_NOTIFICATIONS` permission:
 * [NotificationManagerCompat.areNotificationsEnabled] is checked first, and the `notify` call
 * itself is wrapped defensively in case the permission is revoked in the small window between
 * that check and the call (a `CoroutineWorker` cannot prompt for permission - it can only
 * skip). No deep link: the `PendingIntent` only launches [MainActivity], a scope cut noted in
 * the task write-up rather than an oversight.
 */
@Singleton
class SystemReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderNotifier {

    override fun notifyTask(taskId: Long, title: String) {
        notify(notificationId = (TASK_NOTIFICATION_ID_OFFSET + taskId).toInt(), title = title)
    }

    override fun notifyList(listId: Long, title: String) {
        notify(notificationId = (LIST_NOTIFICATION_ID_OFFSET + listId).toInt(), title = title)
    }

    private fun notify(notificationId: Int, title: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderChannel.ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            manager.notify(notificationId, notification)
        }.onFailure { error ->
            Log.w(TAG, "Could not post reminder notification " + notificationId, error)
        }
    }

    private companion object {
        private const val TAG = "ReminderNotifier"

        // Separate id ranges so a task and a list can never collide on the same
        // notification id even if their raw row ids match.
        private const val TASK_NOTIFICATION_ID_OFFSET = 20_000_000L
        private const val LIST_NOTIFICATION_ID_OFFSET = 30_000_000L
    }
}
