package com.example.myagent.di

import android.content.Context
import androidx.room.Room
import com.example.myagent.data.db.AppDatabase
import com.example.myagent.data.db.dao.MasterFolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME).build()

    @Provides
    fun provideMasterFolderDao(database: AppDatabase): MasterFolderDao =
        database.masterFolderDao()
}