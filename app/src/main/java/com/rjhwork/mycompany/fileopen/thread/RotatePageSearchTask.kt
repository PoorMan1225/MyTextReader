package com.rjhwork.mycompany.fileopen.thread

import android.util.Log
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.util.*
import kotlin.math.roundToInt

class RotatePageSearchTask(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>
) : Runnable {

    private val ratio = if (textViewModel.dataSize > data.size - 1) { // L -> P
        (textViewModel.dataSize.toFloat() / (data.size - 1)) + 0.05
    } else {                                                          // P -> L
        ((data.size - 1).toFloat() / textViewModel.dataSize) - 0.05
    }

    private val maxSearchPage: Int = if (textViewModel.dataSize > data.size - 1) {
        (textViewModel.pagePosition / ratio).roundToInt()
    } else {
        (textViewModel.pagePosition * ratio).roundToInt()
    }

    val maxPage = if (maxSearchPage >= data.size) {
        data.size - 1
    } else maxSearchPage

    val split = textViewModel.currentPageData.split("\n")

    override fun run() {
        // P -> L
        val containList = mutableListOf<Int>()
        var idx = 0

        Log.d(TAG, "maxPage: $maxPage")
        (0..data.size-1).forEach {
            val data = data[it].replace("\n", "")
            val check = data.contains(split[idx])
            if (check) {
                containList.add(it)
            }
        }
        Log.d(TAG, "containList Size : ${containList.size}")
        idx++

        val tempList = mutableListOf<Int>()
        while (containList.size > 1) {
            containList.forEach {
                val data = data[it].replace("\n", "")
                val check = data.contains(split[idx])
                if(check) {
                    tempList.add(it)
                }
            }
            containList.clear()
            containList += tempList
            tempList.clear()
            idx++
        }
        Log.d(TAG, "containList Size : ${containList.size}")
        textViewModel.pagePosition = containList[0]
    }
}