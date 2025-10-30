package com.cardiag.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cardiag.pro.data.model.ECUSystem
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Severity

/**
 * Room entity for storing DTC code definitions.
 */
@Entity(tableName = "dtc_codes")
data class DtcEntity(
    @PrimaryKey
    val code: String,
    val description: String,
    val system: String, // Stored as string, converted to ECUSystem enum
    val severity: String, // Stored as string, converted to Severity enum
    val manufacturer: String? = null // null for generic codes, otherwise manufacturer name
)
