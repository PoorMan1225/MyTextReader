package com.rjhwork.mycompany.fileopen.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlin.math.floor

class TextViewModel : ViewModel() {

    var textCount = 0f
    var maxLine = 0

    // 하면의 가로 넓이 픽셀
    var aWidth = 0
        set(value) {
            field = value
            // width 19 textCount
            textCount = ((aWidth / 54).toFloat())
        }

    // 화면의 세로 넓이 픽셀
    var aHeight = 0
        set(value) {
            field = value
            // height = 14 line
            maxLine = (aHeight / 136)
        }

    var pagePosition = 0

    var currentPageData:String = ""

    var dataSize = 0

    var displayName:String = ""

    var contentUri: Uri? = null
}