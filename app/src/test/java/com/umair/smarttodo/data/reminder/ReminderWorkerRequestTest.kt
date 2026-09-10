package com.umair.smarttodo.data.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the parts of [ReminderWorker]'s scheduling contract that are provably correct as
 * plain JVM logic: the unique-work-name format each id maps to, and the "clamp a past instant
 * to a zero delay, never reject it" rule.
 *
 * What this deliberately does **not** attempt to verify: that
 * [WorkManagerReminderScheduler.scheduleTaskReminder] actually enqueues with
 * `ExistingWorkPolicy.REPLACE`, or that a second call for the same id truly supersedes the
 * first in WorkManager's own persistence. Confirming that needs `androidx.work:work-testing`
 * plus `WorkManagerTestInitHelper` (or a real device), neither of which is available on this
 * machine and neither of which is a dependency of this module yet. A test that merely builds
 * a `OneTimeWorkRequest` and inspects it, without ever calling `enqueueUniqueWork`, would not
 * actually prove the REPLACE semantics either - so rather than write one that looks like
 * coverage without being it, this is stated here plainly instead.
 */
class ReminderWorkerRequestTest {

    @Test
    fun `task and list unique work names never collide for the same numeric id`() {
        assertEquals("reminder-task-42", ReminderWorker.taskUniqueWorkName(42L))
        assertEquals("reminder-list-42", ReminderWorker.listUniqueWorkName(42L))
    }

    @Test
    fun `a future instant yields a positive delay equal to the difference`() {
        val now = 1_000L
        val at = 61_000L

        assertEquals(60_000L, ReminderWorker.computeInitialDelayMillis(at, now))
    }

    @Test
    fun `an instant equal to now yields a zero delay`() {
        assertEquals(0L, ReminderWorker.computeInitialDelayMillis(1_000L, nowMillis = 1_000L))
    }

    @Test
    fun `a past instant is clamped to a zero delay rather than rejected`() {
        assertEquals(0L, ReminderWorker.computeInitialDelayMillis(1_000L, nowMillis = 5_000L))
    }
}
