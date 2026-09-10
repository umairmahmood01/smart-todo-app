package com.umair.smarttodo.di

import com.umair.smarttodo.data.local.TaskDatabase
import com.umair.smarttodo.data.local.TaskListDao
import com.umair.smarttodo.data.reminder.ReminderNotifier
import com.umair.smarttodo.data.reminder.ReminderScheduler
import com.umair.smarttodo.data.reminder.SystemReminderNotifier
import com.umair.smarttodo.data.reminder.WorkManagerReminderScheduler
import com.umair.smarttodo.data.repository.TaskListRepositoryImpl
import com.umair.smarttodo.domain.TaskListRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Constructs the task-list data layer's concrete dependencies: the `task_lists` DAO.
 *
 * `@Provides` because `TaskListDao` needs to be extracted from an existing `TaskDatabase`
 * instance, the same reasoning `DataModule.provideTaskDao` follows for `TaskDao`.
 */
@Module
@InstallIn(SingletonComponent::class)
object TaskListModule {

    /** The DAO is owned by the database, so it inherits the database's singleton lifetime. */
    @Provides
    fun provideTaskListDao(database: TaskDatabase): TaskListDao = database.taskListDao()
}

/**
 * Binds the task-list and reminder layers' implementations to the interfaces the rest of the
 * app depends on. Kept separate from [TaskListModule] only because `@Binds` requires an
 * abstract declaration while `@Provides` requires a concrete one - same split as
 * `DataModule` / `DataBindingsModule`.
 *
 * [ReminderWorker][com.umair.smarttodo.data.reminder.ReminderWorker] itself needs no binding
 * here: `@HiltWorker` generates an assisted factory that `HiltWorkerFactory` picks up, and
 * that factory is installed globally by `SmartTodoApplication.workManagerConfiguration` - the
 * same mechanism already used for `TaskEnrichmentWorker`.
 */
@Module
@InstallIn(SingletonComponent::class)
interface TaskListBindingsModule {

    /** Room-backed single source of truth for task lists. */
    @Binds
    fun bindTaskListRepository(impl: TaskListRepositoryImpl): TaskListRepository

    /** WorkManager-backed scheduling of task/list reminders. */
    @Binds
    fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    /** `NotificationManagerCompat`-backed posting of a fired reminder's notification. */
    @Binds
    fun bindReminderNotifier(impl: SystemReminderNotifier): ReminderNotifier
}
