package com.cardiag.pro.di

import android.content.Context
import com.cardiag.pro.data.connection.BluetoothConnectionAdapter
import com.cardiag.pro.data.connection.ConnectionAdapter
import com.cardiag.pro.data.connection.ConnectionManager
import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.connection.UsbSerialConnectionAdapter
import com.cardiag.pro.data.local.AppDatabase
import com.cardiag.pro.data.local.dao.DiagnosticSessionDao
import com.cardiag.pro.data.local.dao.DtcDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothConnectionAdapter(
        @ApplicationContext context: Context
    ): BluetoothConnectionAdapter {
        return BluetoothConnectionAdapter(context)
    }

    @Provides
    @Singleton
    fun provideUsbSerialConnectionAdapter(
        @ApplicationContext context: Context
    ): UsbSerialConnectionAdapter {
        return UsbSerialConnectionAdapter(context)
    }

    @Provides
    @Singleton
    fun provideConnectionManager(
        bluetoothAdapter: BluetoothConnectionAdapter,
        usbSerialAdapter: UsbSerialConnectionAdapter
    ): ConnectionManager {
        return ConnectionManager(bluetoothAdapter, usbSerialAdapter)
    }

    @Provides
    @Singleton
    fun provideELM327Protocol(
        connectionManager: ConnectionManager
    ): ELM327Protocol {
        return ELM327Protocol(connectionManager)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDtcDao(database: AppDatabase): DtcDao {
        return database.dtcDao()
    }

    @Provides
    @Singleton
    fun provideDiagnosticSessionDao(database: AppDatabase): DiagnosticSessionDao {
        return database.diagnosticSessionDao()
    }
}
