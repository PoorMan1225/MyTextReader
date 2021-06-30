package com.rjhwork.mycompany.fileopen.thread

import android.util.Log
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.math.roundToInt

class RotatePageSearchTask(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>
) : Runnable {

    val split = textViewModel.currentPageData.split("\n")

    override fun run() {

        val containList = mutableListOf<Int>()
        var idx = split.size / 2

        Log.d(TAG, "idx : ${split[idx]}")
        Log.d(TAG, "textViewModel.pagePosition : ${textViewModel.pagePosition}")
        Log.d(TAG, "textViewModel.beforeDataSize : ${textViewModel.beforeDataSize}")
        Log.d(TAG, "data size  : ${data.size-1}")

        while (containList.size < 1) {
            if (textViewModel.beforeDataSize > (data.size - 1)) {
                val ps =
                    if (textViewModel.pagePosition > data.size - 1) data.size - 1 else textViewModel.pagePosition
                (ps downTo 0).forEach { i ->
                    Log.d(TAG, "i : $i")
                    checkProcess(i, idx, containList)
                }
                idx++
            } else {
                (textViewModel.pagePosition until data.size).forEach { i ->
                    Log.d(TAG, "i : $i")
                    checkProcess(i, idx, containList)
                }
                idx++
            }

            if (idx >= split.size) {
                throw IndexOutOfBoundsException("idx 범위 오버.")
            }
        }

        val tempList = mutableListOf<Int>()
        while (containList.size > 1) {
            containList.forEach {
                val data = data[it].replace("\n", "")
                Log.d(TAG, "data : $data")
                val sp = if (split[idx].trim().length > textViewModel.textCount) {
                    split[idx].trim().substring(0, (textViewModel.textCount + 1).toInt())
                } else {
                    split[idx].trim()
                }
                val check = data.contains(sp)
                if (check) {
                    tempList.add(it)
                }
            }
            containList.clear()
            containList += tempList
            tempList.clear()
            idx++
        }
        Log.d(TAG, "containList Size : ${containList.size}")
        Log.d(TAG, "page : ${containList[0]}")
        textViewModel.pagePosition = containList[0]
    }

    private fun checkProcess(
        it: Int,
        idx: Int,
        containList: MutableList<Int>
    ) {
        val data = data[it].replace("\n", "")
        val sp = if (split[idx].length > textViewModel.textCount) {
            split[idx].substring(0, (textViewModel.textCount + 1).toInt()).trim()
        } else {
            split[idx].trim()
        }

        val check = data.contains(sp)
        if (check) {
            containList.add(it)
        }
    }
}