package com.example.myagent.di

import android.content.ContentResolver
import android.content.Context
import com.example.myagent.data.service.MediaScannerService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaScannerModule {
    
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
    
    @Provides
    @Singleton
    fun provideMediaScannerService(
        @ApplicationContext context: Context,
        contentResolver: ContentResolver,
        mediaRepository: com.example.myagent.data.repository.MediaRepository
    ): MediaScannerService {
        return MediaScannerService(context, mediaRepository, contentResolver)
    }
}