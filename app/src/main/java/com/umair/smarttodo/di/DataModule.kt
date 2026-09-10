package com.umair.smarttodo.di

import android.content.Context
import androidx.room.Room
import com.umair.smarttodo.data.categorizer.RuleBasedTaskCategorizer
import com.umair.smarttodo.data.local.Migrations
import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.local.TaskDatabase
import com.umair.smarttodo.data.repository.TaskRepositoryImpl
import com.umair.smarttodo.domain.TaskCategorizer
import com.umair.smarttodo.domain.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Qualifies the [CoroutineDispatcher] used for disk and database work.
 *
 * Consumers depend on this qualifier instead of referencing `Dispatchers.IO` directly, which
 * is what makes them testable: a test can bind a `TestDispatcher` and control execution.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Constructs the data layer's concrete dependencies: the Room database, its DAO and the IO
 * dispatcher.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * The app-wide [TaskDatabase].
     *
     * [Migrations.MIGRATION_1_2] is registered explicitly (no destructive fallback), so a
     * user's existing tasks survive the version 1 -> 2 upgrade. See that object's kdoc for
     * this migration's verification status: reviewed by hand, not yet run on a device.
     */
    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, TaskDatabase.NAME)
            .addMigrations(Migrations.MIGRATION_1_2)
            .build()

    /** The DAO is owned by the database, so it inherits the database's singleton lifetime. */
    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    /** The dispatcher for blocking IO. */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

/**
 * Binds the data layer's implementations to the interfaces the rest of the app depends on.
 *
 * Kept separate from [DataModule] only because `@Binds` requires an abstract declaration
 * while `@Provides` requires a concrete one; conceptually this is the same module.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataBindingsModule {

    /** Room-backed single source of truth for tasks. */
    @Binds
    fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    /** Offline rule-based categorizer; no network dependency. */
    @Binds
    fun bindTaskCategorizer(impl: RuleBasedTaskCategorizer): TaskCategorizer
}
