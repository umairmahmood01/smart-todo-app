package com.umair.smarttodo.di

import com.umair.smarttodo.data.enrichment.EnrichmentScheduler
import com.umair.smarttodo.data.enrichment.WorkManagerEnrichmentScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the background enrichment layer.
 *
 * The worker itself needs no binding: `@HiltWorker` generates an assisted factory that
 * `HiltWorkerFactory` picks up, and that factory is installed by
 * `SmartTodoApplication.workManagerConfiguration`.
 */
@Module
@InstallIn(SingletonComponent::class)
interface EnrichmentModule {

    /** WorkManager-backed scheduling of post-insert enrichment. */
    @Binds
    fun bindEnrichmentScheduler(impl: WorkManagerEnrichmentScheduler): EnrichmentScheduler
}
