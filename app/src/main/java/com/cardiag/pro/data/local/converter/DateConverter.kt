package com.cardiag.pro.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converter for Date objects.
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
