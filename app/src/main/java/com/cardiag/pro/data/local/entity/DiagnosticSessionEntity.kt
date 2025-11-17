package com.cardiag.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cardiag.pro.data.local.converter.DateConverter

/**
 * Room entity for storing diagnostic session history.
 */
@Entity(tableName = "diagnostic_sessions")
@TypeConverters(DateConverter::class)
data class DiagnosticSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val vin: String?,
    val manufacturer: String?,
    val codes: String, // JSON array of detected codes
    val freezeFrames: String? = null, // JSON object mapping code -> freeze frame data
    val notes: String? = null,
    val systemScanned: String? = null // Which ECU system(s) were scanned
)
