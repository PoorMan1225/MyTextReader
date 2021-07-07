package com.rjhwork.mycompany.fileopen.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rjhwork.mycompany.fileopen.model.Data

@Database(entities = [Data::class], version = 1)
@TypeConverters(TextTypeConverters::class)
abstract class TextDataBase: RoomDatabase() {
    abstract fun textDao():TextDao
}