package com.rjhwork.mycompany.fileopen.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlin.math.floor

class TextViewModel : ViewModel() {

    var textCount = 0
    var maxLine = 0

    // 하면의 가로 넓이 픽셀
    var aWidth = 0
        set(value) {
            field = value
            // width 22 textCount
            val xRatio = aWidth * 0.021
            val x = floor((xRatio / aWidth) * 1000) / 1000
            textCount = (aWidth * 0.021).toInt()
        }

    // 화면의 세로 넓이 픽셀
    var aHeight = 0
        set(value) {
            field = value
            // height = 15 line
            val yRatio = aHeight * 0.0078
            val y = floor((yRatio / aHeight) * 1000) / 1000
            maxLine = (aHeight * 0.0078).toInt()
        }

    var pagePosition = 0

    var currentPageData:String = ""

    var dataSize = 0

    var displayName:String = ""

    var contentUri: Uri? = null
}