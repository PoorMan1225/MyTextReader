package com.rjhwork.mycompany.fileopen.thread

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel

class SearchThread(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>,
    private var indexList: MutableList<Int>,
    private val handler: Handler
) : Thread() {

    lateinit var type: String
    var searchLength: Int = 0
    lateinit var searchString: String

    override fun run() {
        Looper.prepare()
        var message: Message? = null

        if (indexList.isNotEmpty()) {
            indexList.clear()
        }

        when (type) {
            "search" -> {

            }
            "forward" -> {
                forwardProcess()
                message = handler.obtainMessage(INDEX_FORWARD_MESSAGE)
            }
            "back" -> {
                backProcess()
                message = handler.obtainMessage(INDEX_BACK_MESSAGE)
            }
        }
        sleep(10)
        message?.let {
            handler.sendMessage(it)
        }
    }

    private fun backProcess() {
        if (textViewModel.pagePosition >= data.size) {
            textViewModel.pagePosition = data.size - 1
        }

        for (i in textViewModel.pagePosition downTo 0) {
            textViewModel.pagePosition--
            val list = getIndexSearchData(data[i], searchLength, searchString) ?: continue

            if (list.isNotEmpty()) {
                indexList += list
                break
            }
        }
    }

    private fun forwardProcess() {
        if (textViewModel.pagePosition < 0) {
            textViewModel.pagePosition = 0
        }

        for (i in textViewModel.pagePosition until data.size) {
            textViewModel.pagePosition++
            val list = getIndexSearchData(data[i], searchLength, searchString) ?: continue

            if (list.isNotEmpty()) {
                indexList += list
                break
            }
        }
    }

    private fun getIndexSearchData(
        txt: String,
        searchLength: Int,
        searchString: String
    ): MutableList<Int>? {
        val list = mutableListOf<Int>()
        var searchIndex = 0
        var idx = txt.indexOf(searchString, searchIndex)

        if (idx != -1) {
            list.add(idx)
            while (true) {
                searchIndex = idx + searchLength
                idx = txt.indexOf(searchString, searchIndex)
                if (idx != -1)
                    list.add(idx)
                else
                    break
            }
            return list
        } else {
            return null
        }
    }

    companion object {
        const val INDEX_FORWARD_MESSAGE = 1002
        const val INDEX_BACK_MESSAGE = 1003

    }
}

