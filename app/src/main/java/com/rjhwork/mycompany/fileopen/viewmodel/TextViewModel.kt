package com.rjhwork.mycompany.fileopen.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlin.math.floor

class TextViewModel : ViewModel() {

    var textCount = 0f
    var maxLine = 0

    // 하면의 가로 넓이 픽셀
    var widthCountRatio = 46
    var heightLineRatio = 129

    var aWidth = 0
        set(value) {
            field = value
            // width 22 textCount
            textCount = ((aWidth / 46).toFloat())
        }

    // 화면의 세로 넓이 픽셀
    var aHeight = 0
        set(value) {
            field = value
            // height = 16 line
            maxLine = (aHeight / 129)
        }

    var pagePosition = 0

    var currentPageData:String = ""

    var dataSize = 0

    var displayName:String = ""

    var contentUri: Uri? = null

    var textSize:Int = 1

    var textSizeDimen:Float = 0f
}