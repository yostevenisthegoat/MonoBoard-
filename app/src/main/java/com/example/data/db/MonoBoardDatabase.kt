package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameStatEntity::class,
        SavedGameEntity::class,
        MatchHistoryEntity::class,
        PlayerProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MonoBoardDatabase : RoomDatabase() {
    abstract fun gameStatDao(): GameStatDao
    abstract fun savedGameDao(): SavedGameDao
    abstract fun matchHistoryDao(): MatchHistoryDao
    abstract fun playerProfileDao(): PlayerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: MonoBoardDatabase? = null

        fun getInstance(context: Context): MonoBoardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MonoBoardDatabase::class.java,
                    "mono_board_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
