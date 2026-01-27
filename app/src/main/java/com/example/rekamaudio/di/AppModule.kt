package com.example.rekamaudio.di

import com.example.rekamaudio.data.repository.AudioCaptureRepository
import com.example.rekamaudio.data.repository.AudioCaptureRepositoryImpl
import com.example.rekamaudio.data.repository.SettingsRepository
import com.example.rekamaudio.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAudioCaptureRepository(
        impl: AudioCaptureRepositoryImpl
    ): AudioCaptureRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
