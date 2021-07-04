package com.rjhwork.mycompany.fileopen.thread

import android.util.Log
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs
import kotlin.math.roundToInt

class RotatePageSearchTask(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>
) : Runnable {

    private val split = textViewModel.currentPageData.split("\n")
    private val list = split.filter { (it.isNotEmpty() && it.isNotBlank() && it.length > 6) }

    override fun run() {

        var idx = 0
        val containList = mutableListOf<Int>()

        Log.d(TAG, "idx : ${list[idx]}")
        Log.d(TAG, "textViewModel.pagePosition : ${textViewModel.pagePosition}")
        Log.d(TAG, "textViewModel.beforeDataSize : ${textViewModel.beforeDataSize}")
        Log.d(TAG, "data size  : ${data.size - 1}")

        while (containList.size < 1) {
            if (textViewModel.beforeDataSize > (data.size - 1)) {
                val ps =
                    if (textViewModel.pagePosition > data.size - 1) data.size - 1 else textViewModel.pagePosition
                (ps downTo 0).forEach { i ->
                    checkProcess(i, idx, containList)
                }
            } else {
                (textViewModel.pagePosition until data.size).forEach { i ->
                    checkProcess(i, idx, containList)
                }
            }
            idx++
        }

        if (idx >= split.size) {
            throw IndexOutOfBoundsException("검색 실패.")
        }

        val ratio = (data.size-1).toFloat() / textViewModel.beforeDataSize
        Log.d(TAG, "ratio : $ratio")
        val ratioPage = (textViewModel.pagePosition * ratio).roundToInt()

        Log.d(TAG, "containList Size : ${containList.size}")
        Log.d(TAG, "ratioPage : ${ratioPage}")

        val absList = containList.map { value -> abs(ratioPage - value) }
        var min = absList[0]
        var position = 0

        absList.forEachIndexed { i, value ->
            if (min > value) {
                min = value
                position = i
            }
        }
        Log.d(TAG, "containList: $containList")
        textViewModel.pagePosition = containList[position]
//        val tempList = mutableListOf<Int>()
//        while (containList.size > 1) {
//            containList.forEach {
//                val data = data[it].replace("\n", "")
//                Log.d(TAG, "data : $data")
//                val sp = if (list[idx].trim().length > textViewModel.textCount)
//                    list[idx].trim().substring(0, (textViewModel.textCount + 1).toInt())
//                 else
//                    list[idx].trim()
//                val check = data.contains(sp)
//                if (check) {
//                    tempList.add(it)
//                }
//            }
//            containList.clear()
//            containList += tempList
//            tempList.clear()
//            idx++
//
//            if(idx >= list.size) {
//
//                return
//            }
//        }
//        Log.d(TAG, "containList Size : ${containList.size}")
//        Log.d(TAG, "page : ${containList[0]}")
//        textViewModel.pagePosition = containList[0]
    }

    private fun checkProcess(
        it: Int,
        idx: Int,
        containList: MutableList<Int>
    ) {
        val data = data[it].replace("\n", "")
        val sp = if (list[idx].length > textViewModel.textCount) {
            list[idx].substring(0, (textViewModel.textCount + 1).toInt()).trim()
        } else {
            list[idx].trim()
        }

        val check = data.contains(sp)
        if (check) {
            containList.add(it)
        }
    }
}