package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MovieMatch::class,
        AppUser::class,
        DeletedRecord::class,
        ConfigRestorePoint::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieMatchDao(): MovieMatchDao
    abstract fun appUserDao(): AppUserDao
    abstract fun deletedRecordDao(): DeletedRecordDao
    abstract fun configRestorePointDao(): ConfigRestorePointDao

    companion object {
        @Volatile
        private var INSTANCE: MovieDatabase? = null

        fun getDatabase(context: Context): MovieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "movie_finder_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
