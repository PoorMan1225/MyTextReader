package com.rjhwork.mycompany.fileopen.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.rjhwork.mycompany.fileopen.model.Data
import com.rjhwork.mycompany.fileopen.thread.ThreadPoolManager
import java.lang.IllegalStateException

private val DATABASE_NAME = "text-database"

class TextRepository private constructor(context: Context) {

    private val database: TextDataBase = Room.databaseBuilder(
        context.applicationContext,
        TextDataBase::class.java,
        DATABASE_NAME
    ).build()

    private val textDao = database.textDao()

    fun getDataList(): LiveData<List<Data>> = textDao.getDataList()

    fun updateData(data: Data) = textDao.updateData(data)

    fun addData(data: Data) = textDao.addData(data)

    fun getData(uri:String) = textDao.getData(uri)

    fun deleteData(data:Data) = textDao.deleteData(data)

    companion object {
        private var INSTANCE: TextRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = TextRepository(context)
            }
        }

        fun get(): TextRepository {
            return INSTANCE ?: throw IllegalStateException("TextRepository must be initialized")
        }
    }
}