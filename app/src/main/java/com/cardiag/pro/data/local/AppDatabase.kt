package com.cardiag.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cardiag.pro.data.local.converter.DateConverter
import com.cardiag.pro.data.local.dao.DiagnosticSessionDao
import com.cardiag.pro.data.local.dao.DtcDao
import com.cardiag.pro.data.local.entity.DiagnosticSessionEntity
import com.cardiag.pro.data.local.entity.DtcEntity

/**
 * Main Room database for CarDiag Pro.
 */
@Database(
    entities = [
        DtcEntity::class,
        DiagnosticSessionEntity::class
    ],
    version = 3, // Bumped for freeze frame data fields
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dtcDao(): DtcDao
    abstract fun diagnosticSessionDao(): DiagnosticSessionDao

    companion object {
        private const val DATABASE_NAME = "cardiag_pro.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
