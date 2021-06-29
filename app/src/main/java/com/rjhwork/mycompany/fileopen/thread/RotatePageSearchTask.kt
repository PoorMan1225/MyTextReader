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

    val split = textViewModel.currentPageData.split("\n")

    override fun run() {
        // P -> L
        val containList = mutableListOf<Int>()
        var idx = split.size / 2

        (0 until data.size).forEach {
            val data = data[it].replace("\n", "")
            val sp = if (split[idx].length > textViewModel.textCount) {
                split[idx].substring(0, (textViewModel.textCount + 1).toInt())
            } else {
                split[idx].trim()
            }

            val check = data.contains(sp)
            if (check) {
                containList.add(it)
            }
        }
        Log.d(TAG, "data : ${split[idx].trim()}")
        idx++

        val tempList = mutableListOf<Int>()
        while (containList.size > 1) {
            containList.forEach {
                val data = data[it].replace("\n", "")
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
}