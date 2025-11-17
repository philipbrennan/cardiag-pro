package com.cardiag.pro

import android.app.Application
import com.cardiag.pro.data.local.DatabaseInitializer
import com.cardiag.pro.data.logging.FileLogger
import com.cardiag.pro.data.logging.FileLoggingTree
import com.cardiag.pro.data.logging.LogRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CarDiagApp : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var fileLogger: FileLogger

    @Inject
    lateinit var logRepository: LogRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Plant file logging tree for production logging
        Timber.plant(FileLoggingTree(fileLogger, logRepository))

        Timber.d("CarDiag Pro initialized")

        // Initialize database with DTC codes
        applicationScope.launch {
            databaseInitializer.initializeIfNeeded(this@CarDiagApp)
        }
    }
}
