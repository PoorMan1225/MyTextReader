package com.rjhwork.mycompany.fileopen.thread

import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rjhwork.mycompany.fileopen.TAG
import com.rjhwork.mycompany.fileopen.viewmodel.TextViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

class TextSplitThread(
    private val uri: Uri,
    private val encoding: String,
    private val context: Context,
    private val textViewModel: TextViewModel,
    private val data: MutableList<String>,
    private val handler: Handler
) : Thread() {

    companion object {
        const val MESSAGE_TEXT_TYPE = 1001
        var saveSB = StringBuilder()
        var saveCount = 0f
    }

    override fun run() {
        Looper.prepare()
        val resultList = mutableListOf<String>()
        val saveList = mutableListOf<String>()
        val sb = StringBuilder()

        context.contentResolver.openInputStream(uri).use { inputStream ->

            BufferedReader(InputStreamReader(inputStream, encoding)).use { reader ->
                var line: String? = reader.readLine()
                var count: Int

                while (line != null) {
                    addSaveListToResultList(saveList, resultList)
                    // text 문서의 한 라인을 화면의 넓이에 맞기 파싱한 라인 list 를
                    // 결과 리스트에 더한다.
                    addLineListToResultList(line, resultList, textViewModel)

                    count = resultList.size
                    // 결과 리스트의 사이즈가 화면 높이의 maxLine 보다 더 작은 경우
                    if (count < textViewModel.maxLine) {
                        line = reader.readLine()
                        // 마지막일 경우 add 하고 끝.
                        if (line == null) {
                            resultList.forEachIndexed { i, _ ->
                                sb.append(resultList[i])
                            }
                            val offset = textViewModel.maxLine - resultList.size
                            (0 until offset).forEach { _ ->
                                sb.append("\n")
                            }
                            data.add(sb.toString())
                            break
                        }
                        continue
                    } else {
                        // 결과 리스트의 사이즈가 화면 높이의 maxLine 보다 더 큰 경우
                        // maxLine 에 맞게 잘라준다.
                        splitHeightLine(sb, resultList, count, saveList)
                    }
                    line = reader.readLine()
                }
            }
        }
        val message = handler.obtainMessage(MESSAGE_TEXT_TYPE)
        handler.sendMessage(message)
    }

    private fun splitHeightLine(
        sb: StringBuilder,
        resultList: MutableList<String>,
        count: Int,
        saveList: MutableList<String>
    ) {
        var count1 = count
        var i = 0
        val j = textViewModel.maxLine - 1

        while (true) {
            (i..(j + i)).forEach {
                sb.append(resultList[it])
            }
            data.add(sb.toString())
            sb.clear()

            count1 -= textViewModel.maxLine
            i += j + 1
            if (count1 < textViewModel.maxLine) {
                break
            }
        }
        (i until resultList.size).forEach {
            saveList.add(resultList[it])
        }
        resultList.clear()
    }

    private fun addLineListToResultList(
        line: String,
        resultList: MutableList<String>,
        textViewModel: TextViewModel
    ) {
        val lineList = checkTextCountAddLineList(line, textViewModel)
        resultList += lineList
    }

    private fun addSaveListToResultList(
        saveList: MutableList<String>,
        resultList: MutableList<String>
    ) {
        if (saveList.size > 0) {
            resultList += saveList
            saveList.clear()
        }
    }

    // 문자를 한글자 씩 확인해서 width 에 맞게 textCount 센후에 해당 문자 한줄씩
// lineList 에 넣어주는 함수
    private fun checkTextCountAddLineList(
        line: String,
        textViewModel: TextViewModel
    ): MutableList<String> {
        var getLine = line
        val lineList = mutableListOf<String>()

        if(getLine.isNotEmpty()
            && getLine.length <= textViewModel.textCount
            && saveSB.isEmpty()
        ) {
            lineList.add("$getLine\n")
            return lineList
        }

        Log.d(TAG, "saveSB : ${saveSB.toString()}")

        var count = if (saveSB.isNotEmpty()) saveCount else 0f
        val sb = getSaveStringBuilder()


        Log.d(TAG, "line : $getLine")

        if (getLine.isEmpty() && sb.isNotEmpty()) {
            getLine = sb.toString()
            sb.clear()
            count = 0f
        }

        var i = 0
        while (i < getLine.length) {
            val c = getLine[i]
            count += checkWidth(c)

            if (i == getLine.length - 1) {
                if (i <= textViewModel.textCount) {
                    sb.append(c).append("\n")
                    lineList.add(sb.toString())
                } else {
                    sb.append(c)
                    saveSB.append(sb.toString())
                    saveCount = count
                }
                break
            }

            sb.append(c)

            if (count >= textViewModel.textCount) {
                count = 0f
                val l = getLine[i + 1]
                if (l == '.' || l == '!' || l == '?' || l == '"' || l == '“' || l == ',') {
                    sb.append(l).append("\n")
                    i += 1
                } else {
                    sb.append("\n")
                }
                lineList.add(sb.toString())
                sb.clear()
            }
            i++
        }
        return lineList
    }

    private fun checkWidth(c: Char): Float {
        return when {
            ((c in 'a'..'z') || (c in '0'..'9') || (c in 'A'..'Z') || c == '^' || c == '~') -> 0.63f
            (c == '*' || c == '_' || c == '-' || c == '+' || c == '"' || c == ' ' || c == '?' || c == '/' || c == '”') -> 0.84f
            (c == ',' || c == '\'' || c == '`' || c == '.' || c == ':' || c == ';' || c == '|') -> 0.23f
            (c == '!' || c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']') -> 0.33f
            (c == '─') -> 1.1f
            else -> 1.0f
        }
    }

    private fun getSaveStringBuilder(): StringBuilder {
        return if (saveSB.isNotEmpty()) {
            val sb = StringBuilder().append(saveSB.toString())
            saveSB.clear()
            sb
        } else {
            StringBuilder()
        }
    }
}


