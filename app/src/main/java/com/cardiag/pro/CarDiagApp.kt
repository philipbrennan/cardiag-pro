package com.cardiag.pro

import android.app.Application
import com.cardiag.pro.data.local.DatabaseInitializer
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("CarDiag Pro initialized")

        // Initialize database with DTC codes
        applicationScope.launch {
            databaseInitializer.initializeIfNeeded(this@CarDiagApp)
        }
    }
}
