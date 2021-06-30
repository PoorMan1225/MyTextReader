package com.rjhwork.mycompany.fileopen.thread

import android.util.Log
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel

class PageSearchTask(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>
) : Runnable {
    private val split = textViewModel.currentPageData.split("\n")

    override fun run() {
        val pagePosition = if (textViewModel.beforeDataSize > (data.size - 1)) {
            largeToSmall()
        } else {
            smallToLarge()
        }

        if (pagePosition == -1) {
            throw NullPointerException()
        }
        Log.d(TAG, "pagePosition : $pagePosition")
//        Log.d(TAG, "searchData : ${data[pagePosition]}")
        textViewModel.pagePosition = pagePosition
    }

    private fun largeToSmall(): Int {
        var checkCount = 0

        (textViewModel.pagePosition downTo 0).forEach { i ->
            val data = data[i].replace("\n", "")

            split.forEach { line ->
                Log.d(TAG, "line : $line")
                if(line != "\n") {
                    val check = if (line.length > (textViewModel.textCount - 1)) {
                        val sLine = line.substring(0, (textViewModel.textCount - 1).toInt())
                        data.contains(sLine.trim())
                    } else {
                        data.contains(line.trim())
                    }

                    if (check) {
                        checkCount++
                    }
                }
            }
            if(checkCount < 2) {
                checkCount = 0
            }
            Log.d(TAG, "checkCount : $checkCount")
            if (checkCount >= 3) {
                Log.d(TAG, "searchData : ${data[i]}")
                return i
            }
        }
        return -1
    }

    private fun smallToLarge(): Int {
        var checkCount = 0

        (textViewModel.pagePosition until data.size).forEach { i ->
            val data = data[i].replace("\n", "")
            Log.d(TAG, "data : $data")

            split.forEach { line ->
                Log.d(TAG, "line : $line")
                if(line.isNotEmpty() && line.isNotBlank()) {
                    val check = if (line.length > (textViewModel.textCount - 1)) {
                        val sLine = line.substring(0, (textViewModel.textCount - 1).toInt())
                        data.contains(sLine.trim())
                    } else {
                        data.contains(line.trim())
                    }

                    if (check) {
                        checkCount++
                    }
                }
            }

            if(checkCount < 2) {
                checkCount = 0
            }
            Log.d(TAG, "checkCount : $checkCount")
            if (checkCount >= 3) {
                Log.d(TAG, "searchData : $data")
                return i
            }
        }
        return -1
    }
}