package com.rjhwork.mycompany.fileopen.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rjhwork.mycompany.fileopen.model.Data

@Dao
interface TextDao {
    @Query("SELECT * FROM data")
    fun getDataList(): LiveData<List<Data>>

    @Query("SELECT * FROM data WHERE uri=(:uri)")
    fun getData(uri:String):Data?

    @Update
    fun updateData(data:Data)

    @Insert
    fun addData(data:Data)

    @Delete
    fun deleteData(data:Data)
}