package com.rjhwork.mycompany.fileopen.database

import androidx.room.TypeConverter
import java.util.*

class TextTypeConverters {

    @TypeConverter
    fun fromDate(date: Date?) : Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long?) : Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }
}