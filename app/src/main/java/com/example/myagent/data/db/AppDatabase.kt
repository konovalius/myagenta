package com.example.myagent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myagent.data.db.dao.MasterFolderDao
import com.example.myagent.data.db.dao.MediaDao
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Media

@Database(
    entities = [MasterFolder::class, Media::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterFolderDao(): MasterFolderDao

    abstract fun mediaDao(): MediaDao

    companion object {
        const val DB_NAME = "myagent.db"

        fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}