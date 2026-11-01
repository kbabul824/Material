package com.meditation.tracker.di

import com.meditation.tracker.data.repository.MeditationRepository
import com.meditation.tracker.data.repository.MeditationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMeditationRepository(
        impl: MeditationRepositoryImpl
    ): MeditationRepository
}
