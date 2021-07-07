package com.rjhwork.mycompany.fileopen.viewmodel

import androidx.lifecycle.ViewModel
import com.rjhwork.mycompany.fileopen.database.TextRepository
import com.rjhwork.mycompany.fileopen.model.Data

class BookMarkViewModel: ViewModel() {

    private val textRepository: TextRepository = TextRepository.get()

    val dataList = textRepository.getDataList()

    fun deleteData(data: Data) = textRepository.deleteData(data)
}