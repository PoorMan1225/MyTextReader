package com.rjhwork.mycompany.fileopen

class SearchTask(
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>,
    private var indexList: MutableList<Int>,
):Runnable {
    lateinit var type: String
    var searchLength: Int = 0
    lateinit var searchString: String

    override fun run() {

        if (indexList.isNotEmpty()) {
            indexList.clear()
        }

        when (type) {
            "search" -> currentSearch()
            "forward" -> forwardProcess()
            "back" -> backProcess()
        }
        Thread.sleep(10)
    }

    private fun currentSearch() {
        val list =  getIndexSearchData(data[textViewModel.pagePosition], searchLength, searchString) ?: return
        if(list.isNotEmpty()) {
            indexList += list
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
}